package com.savory.market.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.context.BaseContext;
import com.savory.common.constant.MessageConstant;
import com.savory.common.exception.OrderBusinessException;
import com.savory.common.result.PageResult;
import com.savory.market.dto.SeckillBuyDTO;
import com.savory.market.mapper.SeckillActivityMapper;
import com.savory.market.service.SeckillService;
import com.savory.pojo.entity.SeckillActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 秒杀服务实现类
 *
 * 完整秒杀链路:
 * 1. 活动创建 → 库存预热到 Redis
 * 2. 用户秒杀: Redis Lua 脚本原子执行 (校验时间 + 查重 + 扣库存)
 * 3. 异步落库: RocketMQ 消息 → 消费者创建订单
 * 4. 超时回补: RocketMQ延迟消息 → 回补库存 + 释放优惠券
 */
@DS("market")
@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //Redis Lua脚本: 原子执行秒杀逻辑
    private static final String SECKILL_LUA_SCRIPT =
            "-- 秒杀Lua脚本: 校验时间窗口 + 查重 + 扣库存\n" +
            "local stockKey = KEYS[1]       -- seckill:stock:{activityId}:{dishId}\n" +
            "local userKey = KEYS[2]        -- seckill:users:{activityId}\n" +
            "local userId = ARGV[1]\n" +
            "local limitPerUser = tonumber(ARGV[2])\n" +
            "\n" +
            "-- 1. 检查库存\n" +
            "local stock = tonumber(redis.call('GET', stockKey) or '0')\n" +
            "if stock <= 0 then\n" +
            "    return -1  -- 库存不足\n" +
            "end\n" +
            "\n" +
            "-- 2. 检查是否重复下单\n" +
            "local userCount = redis.call('SISMEMBER', userKey, userId)\n" +
            "if userCount == 1 then\n" +
            "    return -2  -- 重复秒杀\n" +
            "end\n" +
            "\n" +
            "-- 3. 扣减库存\n" +
            "redis.call('DECR', stockKey)\n" +
            "\n" +
            "-- 4. 记录用户（防重）\n" +
            "redis.call('SADD', userKey, userId)\n" +
            "\n" +
            "return 1  -- 秒杀成功";

    @Override
    @Transactional
    public void createActivity(SeckillActivity activity) {
        //1、保存秒杀活动
        activity.setStatus(0); //未开始
        seckillActivityMapper.insert(activity);
        log.info("创建秒杀活动成功，activityId: {}", activity.getId());

        //2、活动库存预热到Redis（活动开始前5分钟由定时任务执行）
        //TODO: 预热逻辑由 @Scheduled 定时任务处理
    }

    @Override
    public PageResult pageActivity(int page, int pageSize) {
        //1、构建分页条件
        Page<SeckillActivity> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SeckillActivity::getCreateTime);

        //2、执行分页查询
        Page<SeckillActivity> result = seckillActivityMapper.selectPage(p, wrapper);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    @Override
    public List<SeckillActivity> listRunning() {
        //1、查询进行中的秒杀活动
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillActivity::getStatus, 1);  //进行中
        return seckillActivityMapper.selectList(wrapper);
    }

    @Override
    public SeckillActivity getActivityById(Long id) {
        //1、查询秒杀活动详情
        return seckillActivityMapper.selectById(id);
    }

    @Override
    public Long seckillBuy(SeckillBuyDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Long activityId = dto.getActivityId();

        //1、查询秒杀活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new OrderBusinessException("秒杀活动不存在");
        }

        //2、服务端二次校验时间窗口
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new OrderBusinessException(MessageConstant.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new OrderBusinessException(MessageConstant.SECKILL_ENDED);
        }

        //3、执行Lua脚本进行原子秒杀
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(SECKILL_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        String stockKey = "seckill:stock:" + activityId + ":" + dto.getDishId();
        String userKey = "seckill:users:" + activityId;

        Long result = redisTemplate.execute(
                redisScript,
                Arrays.asList(stockKey, userKey),
                userId.toString(),
                activity.getLimitPerUser().toString()
        );

        //4、根据Lua脚本返回值处理结果
        if (result == -1) {
            throw new OrderBusinessException(MessageConstant.SECKILL_STOCK_OUT);
        } else if (result == -2) {
            throw new OrderBusinessException(MessageConstant.SECKILL_REPEAT);
        }

        log.info("秒杀成功，userId: {}, activityId: {}, dishId: {}", userId, activityId, dto.getDishId());

        //5、发送RocketMQ消息异步创建订单
        //TODO: producer.send(seckillOrderMessage)

        //6、此处返回预占的订单标识，实际订单在MQ消费者中创建
        return userId; // 临时返回userId
    }
}
