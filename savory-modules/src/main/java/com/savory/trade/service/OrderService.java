package com.savory.trade.service;

import com.savory.market.seckill.mq.SeckillMessage;
import com.savory.trade.dto.OrderSubmitDTO;
import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Orders;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 用户提交订单
     */
    Orders submit(OrderSubmitDTO orderSubmitDTO);

    /**
     * 用户取消订单
     */
    void cancel(Long orderId, Long userId);

    /**
     * 用户支付订单（渠道支付）
     */
    void pay(Long orderId, Long userId, String channelCode);

    /**
     * 商家接单
     */
    void confirm(Long orderId);

    /**
     * 商家拒单
     */
    void reject(Long orderId, String reason);

    /**
     * 完成订单
     */
    void complete(Long orderId);

    /**
     * 退款处理
     */
    void refund(Long orderId);

    /**
     * 分页查询订单
     */
    PageResult pageQuery(Integer page, Integer pageSize, Integer status);

    /**
     * 创建秒杀订单（trade 库建单，uk_user_activity 防重）
     */
    Long createSeckillOrder(SeckillMessage message);
}
