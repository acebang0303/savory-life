package com.savory.trade.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息生产者/消费者
 *
 * 生产场景:
 * 1. 下单成功后 → 延迟消息(15分钟) → 检查支付状态
 * 2. 秒杀成功后 → 异步创建订单
 *
 * 消费场景:
 * 1. 延迟消息到期 → 如果未支付 → 自动取消
 * 2. 秒杀消费 → 创建订单 + 扣减MySQL库存
 */
@Component
@Slf4j
public class OrderMessageProducer {

    // TODO: 接入 RocketMQ 原生SDK Producer
    // private final Producer producer;

    /**
     * 发送订单支付超时检查消息（延迟15分钟）
     */
    public void sendOrderDelayCheck(Long orderId) {
        log.info("发送订单延迟检查消息: orderId={}, delayLevel=3(15min)", orderId);

        // TODO: RocketMQ延迟消息
        // Message message = new Message("order-delay-topic", String.valueOf(orderId).getBytes());
        // message.setDelayTimeLevel(3); // 15分钟
        // producer.send(message);
    }

    /**
     * 发送秒杀订单异步创建消息
     */
    public void sendSeckillOrder(Long userId, Long activityId, Long dishId) {
        log.info("发送秒杀订单消息: userId={}, activityId={}, dishId={}", userId, activityId, dishId);

        // TODO: RocketMQ普通消息
        // String body = JSON.toJSONString(Map.of("userId", userId, "activityId", activityId, "dishId", dishId));
        // Message message = new Message("seckill-order-topic", body.getBytes());
        // producer.send(message);
    }
}
