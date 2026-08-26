package com.savory.trade.mq;

import com.alibaba.fastjson2.JSON;
import com.savory.market.seckill.mq.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 消息生产者。
 */
@Component
@Slf4j
public class OrderMessageProducer {

    private static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";

    private final DefaultMQProducer rocketMQProducer;

    public OrderMessageProducer(DefaultMQProducer rocketMQProducer) {
        this.rocketMQProducer = rocketMQProducer;
    }

    /**
     * 发送订单支付超时检查消息（延迟15分钟）
     */
    public void sendOrderDelayCheck(Long orderId) {
        log.info("发送订单延迟检查消息: orderId={}, delayLevel=3(15min)", orderId);
        // TODO: RocketMQ延迟消息
    }

    /**
     * 发送秒杀订单异步创建消息
     */
    public void sendSeckillOrder(SeckillMessage message) {
        try {
            Message msg = new Message(SECKILL_ORDER_TOPIC,
                    JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8));
            rocketMQProducer.send(msg);
            log.info("发送秒杀订单消息: orderNo={}", message.orderNo());
        } catch (Exception e) {
            log.warn("发送秒杀订单消息失败 orderNo={}: {}", message.orderNo(), e.getMessage());
        }
    }
}
