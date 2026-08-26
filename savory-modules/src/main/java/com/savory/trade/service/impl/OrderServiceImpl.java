package com.savory.trade.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.context.BaseContext;
import com.savory.common.exception.OrderBusinessException;
import com.savory.common.result.PageResult;
import com.savory.market.seckill.mq.SeckillMessage;
import com.savory.merchant.mapper.DishMapper;
import com.savory.pojo.entity.*;
import com.savory.trade.dto.OrderSubmitDTO;
import com.savory.trade.mapper.OrderDetailMapper;
import com.savory.trade.mapper.OrderMapper;
import com.savory.trade.mq.NotifyMessageProducer;
import com.savory.trade.pay.core.model.RefundResult;
import com.savory.trade.pay.service.PayOrderService;
import com.savory.trade.service.OrderService;
import com.savory.user.mapper.AddressBookMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 核心流程：下单（Redisson锁防重 + RocketMQ延迟消息） → 支付 → 状态流转
 */
@DS("trade")
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private NotifyMessageProducer notifyMessageProducer;

    /**
     * 用户提交订单（核心流程）
     *
     * @param orderSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public Orders submit(OrderSubmitDTO orderSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        //1、使用Redisson分布式锁防止同一用户并发重复提交
        RLock lock = redissonClient.getLock("order:lock:" + userId);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new OrderBusinessException("系统繁忙，请稍后再试");
            }

            //2、查询收货地址
            AddressBook address = addressBookMapper.selectById(orderSubmitDTO.getAddressBookId());
            if (address == null) {
                throw new OrderBusinessException("收货地址为空");
            }

            //3、生成订单号（雪花算法）
            String orderNumber = IdUtil.getSnowflakeNextIdStr();

            //4、构建订单实体
            Orders order = Orders.builder()
                    .number(orderNumber)
                    .userId(userId)
                    .merchantId(orderSubmitDTO.getMerchantId())
                    .addressBookId(orderSubmitDTO.getAddressBookId())
                    .addressDetail(address.getProvinceName() + address.getCityName()
                            + address.getDistrictName() + address.getDetail())
                    .amount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .deliveryFee(BigDecimal.ZERO)
                    .payAmount(BigDecimal.ZERO)
                    .payMethod(orderSubmitDTO.getPayMethod())
                    .payStatus(Orders.UN_PAID)
                    .status(Orders.PENDING_PAYMENT)
                    .remark(orderSubmitDTO.getRemark())
                    .build();
            orderMapper.insert(order);

            //5、创建订单明细（从Redis购物车读取并计算金额）
            BigDecimal totalAmount = createOrderDetailsAndCalculate(order.getId(), userId);

            //6、更新订单金额
            order.setAmount(totalAmount);
            order.setPayAmount(totalAmount.subtract(order.getDiscountAmount()));
            orderMapper.updateById(order);

            //7、清空购物车
            redisTemplate.delete("cart:" + userId);

            //8、发送RocketMQ延迟消息（15分钟后检查支付状态）
            sendDelayCheckMessage(order.getId());

            log.info("订单提交成功，orderId: {}, orderNumber: {}, amount: {}",
                    order.getId(), orderNumber, totalAmount);
            return order;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderBusinessException("系统异常");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 创建订单明细并计算总金额
     */
    private BigDecimal createOrderDetailsAndCalculate(Long orderId, Long userId) {
        BigDecimal total = BigDecimal.ZERO;

        //1、从Redis购物车读取数据
        String cartKey = "cart:" + userId;
        Map<Object, Object> cartEntries = redisTemplate.opsForHash().entries(cartKey);

        if (cartEntries.isEmpty()) {
            log.warn("购物车为空，userId: {}", userId);
            return total;
        }

        //2、遍历购物车条目创建OrderDetail
        for (Map.Entry<Object, Object> entry : cartEntries.entrySet()) {
            String json = (String) entry.getValue();
            com.alibaba.fastjson2.JSONObject item = com.alibaba.fastjson2.JSON.parseObject(json);

            OrderDetail detail = OrderDetail.builder()
                    .orderId(orderId)
                    .name(item.getString("name"))
                    .image(item.getString("image"))
                    .dishFlavor(item.getString("dishFlavor"))
                    .amount(item.getBigDecimal("amount"))
                    .number(item.getIntValue("number"))
                    .build();
            orderDetailMapper.insert(detail);

            //3、累加金额
            BigDecimal itemTotal = detail.getAmount().multiply(BigDecimal.valueOf(detail.getNumber()));
            total = total.add(itemTotal);
        }

        log.info("订单明细创建完成，orderId: {}, 明细数: {}, 总金额: {}", orderId, cartEntries.size(), total);
        return total;
    }

    /**
     * 发送RocketMQ延迟消息检查支付状态
     */
    private void sendDelayCheckMessage(Long orderId) {
        //TODO: 集成RocketMQ原生SDK发送延迟消息
        // 生产环境实现：
        // Message message = new Message("ORDER_DELAY_TOPIC", String.valueOf(orderId).getBytes());
        // message.setDelayTimeLevel(3); // RocketMQ延迟级别3 = 15分钟
        // rocketMQProducer.send(message);
        log.info("发送订单支付延迟检查消息（RocketMQ），orderId: {}", orderId);
    }

    @Override
    @Transactional
    public void cancel(Long orderId, Long userId) {
        //1、查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不存在");
        }

        //2、只有待支付状态可以取消
        if (!order.getStatus().equals(Orders.PENDING_PAYMENT)) {
            throw new OrderBusinessException("当前订单状态不允许取消");
        }

        //3、更新订单状态
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户主动取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单已取消，orderId: {}", orderId);
    }

    @Override
    @Transactional
    public void pay(Long orderId, Long userId, String channelCode) {
        //1、查询并校验订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不存在");
        }
        if (!order.getStatus().equals(Orders.PENDING_PAYMENT)) {
            throw new OrderBusinessException("订单状态异常");
        }

        //2、委托支付中台：创建支付单 → 渠道下单
        // 余额/mock 渠道同步入账，状态由 OrderPaidConsumer 回写；微信渠道返回支付参数待回调
        payOrderService.createPayOrder(order.getNumber(), channelCode, order.getPayAmount(), userId);
    }

    @Override
    @Transactional
    public void confirm(Long orderId) {
        //1、查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }

        //2、校验订单状态（只有待接单状态可以接单）
        if (!order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException("当前订单状态不可接单");
        }

        //3、更新状态
        order.setStatus(Orders.PREPARING);
        orderMapper.updateById(order);
        log.info("商家接单，orderId: {}", orderId);

        //4、定向推送通知用户"商家已接单"
        notifyMessageProducer.sendToUser(order.getUserId(), "接单", "您的订单已被商家接单");
    }

    @Override
    @Transactional
    public void reject(Long orderId, String reason) {
        //1、查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }

        //2、校验状态
        if (!order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException("当前订单状态不可拒单");
        }

        //3、更新为已取消
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason(reason != null ? reason : "商家拒单");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("商家拒单，orderId: {}, reason: {}", orderId, reason);

        //4、TODO: 触发退款流程 + 回补库存
    }

    @Override
    @Transactional
    public void complete(Long orderId) {
        //1、查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }

        //2、校验状态
        if (!order.getStatus().equals(Orders.AWAITING_PICKUP)) {
            throw new OrderBusinessException("当前订单状态不可完成");
        }

        //3、更新为已完成
        order.setStatus(Orders.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单已完成，orderId: {}", orderId);
    }

    @Override
    @Transactional
    public void refund(Long orderId) {
        //1、查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }

        //2、只允许已支付或已完成的订单退款
        if (!order.getPayStatus().equals(Orders.PAID)
                && !order.getStatus().equals(Orders.COMPLETED)) {
            throw new OrderBusinessException("当前订单状态不可退款");
        }

        //3、委托支付中台退款（余额渠道退回账户，微信渠道骨架）
        RefundResult result = payOrderService.refund(order, "用户退款");
        if (!result.success()) {
            throw new OrderBusinessException(result.message());
        }

        //4、更新退款状态
        order.setStatus(Orders.REFUNDED);
        order.setPayStatus(Orders.REFUND);
        orderMapper.updateById(order);
        log.info("退款处理完成，orderId: {}", orderId);
    }

    @Override
    public PageResult pageQuery(Integer page, Integer pageSize, Integer status) {
        //1、构建分页条件
        Page<Orders> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Orders::getStatus, status)
               .orderByDesc(Orders::getCreateTime);

        //2、执行分页查询
        Page<Orders> result = orderMapper.selectPage(p, wrapper);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 定时任务：处理超时未支付订单（由延迟消息消费者调用）
     */
    @Transactional
    public void handleTimeoutOrder(Long orderId) {
        //1、查询订单当前状态
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getStatus().equals(Orders.PENDING_PAYMENT)) {
            return; //订单已被支付或取消，无需处理
        }

        //2、超时取消订单
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("支付超时，系统自动取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("超时订单已自动取消，orderId: {}", orderId);

        //3、TODO: 如果使用了优惠券，需释放优惠券
        //4、TODO: 如果是秒杀订单，需回补Redis库存
    }

    @Override
    @Transactional
    public Long createSeckillOrder(SeckillMessage message) {
        //1、从菜品查 merchantId（秒杀订单必须关联商户）
        Dish dish = dishMapper.selectById(message.dishId());
        if (dish == null) {
            throw new OrderBusinessException("秒杀菜品不存在");
        }

        //2、组装秒杀订单
        Orders order = Orders.builder()
                .number(message.orderNo())
                .userId(message.userId())
                .merchantId(dish.getMerchantId())
                .amount(message.payAmount())
                .discountAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .payAmount(message.payAmount())
                .payStatus(Orders.UN_PAID)
                .status(Orders.PENDING_PAYMENT)
                .isSeckill(1)
                .seckillActivityId(message.activityId())
                .build();

        //3、插入订单，uk_user_activity 唯一索引防重复秒杀
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new OrderBusinessException("重复秒杀");
        }
        return order.getId();
    }
}
