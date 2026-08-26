package com.savory.market.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.savory.common.exception.OrderBusinessException;
import com.savory.market.service.SeckillService;
import com.savory.trade.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 秒杀下单消费者：Redis 已预扣库存，这里做 MySQL 兜底扣减与建单（削峰落库）。
 * 确定性失败（库存不足/重复单）回滚 Redis 后确认；瞬时失败重试一次。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderCreateConsumer {

    private static final String TOPIC = "seckill-order-topic";

    @Value("${rocketmq.name-server:localhost:9876}")
    private String nameServer;

    @Value("${rocketmq.consumer.group:seckill-order-consumer}")
    private String consumerGroup;

    private final SeckillService seckillService;
    private final OrderService orderService;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void start() throws MQClientException {
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(TOPIC, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, ctx) -> {
            for (MessageExt msg : msgs) {
                SeckillMessage message = JSON.parseObject(
                        new String(msg.getBody(), StandardCharsets.UTF_8), SeckillMessage.class);
                boolean deducted = false;
                try {
                    // 1. market 库 CAS 扣库存
                    deducted = seckillService.deductStock(message.activityId(), message.quantity());
                    if (!deducted) {
                        seckillService.revertRedisStock(message.activityId(), message.dishId(),
                                message.userId(), message.quantity());
                        log.warn("库存不足，回滚Redis: orderNo={}", message.orderNo());
                        continue;
                    }
                    // 2. trade 库建单
                    orderService.createSeckillOrder(message);
                } catch (OrderBusinessException e) {
                    // 确定性失败（重复单等）：回补库存 + 回滚 Redis 后确认，不重试
                    if (deducted) {
                        seckillService.restoreStock(message.activityId(), message.quantity());
                    }
                    seckillService.revertRedisStock(message.activityId(), message.dishId(),
                            message.userId(), message.quantity());
                    log.warn("建单确定性失败，回补库存并回滚Redis: orderNo={}, reason={}",
                            message.orderNo(), e.getMessage());
                } catch (Exception e) {
                    // 瞬时失败：回补库存 + 回滚 Redis；已重试一次仍失败转人工，否则重试
                    if (deducted) {
                        seckillService.restoreStock(message.activityId(), message.quantity());
                    }
                    seckillService.revertRedisStock(message.activityId(), message.dishId(),
                            message.userId(), message.quantity());
                    if (msg.getReconsumeTimes() >= 1) {
                        log.error("建单瞬时失败且已重试，转人工: orderNo={}", message.orderNo(), e);
                    } else {
                        log.warn("建单瞬时失败，进入重试: orderNo={}", message.orderNo(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("SeckillOrderCreateConsumer 启动成功, topic={}", TOPIC);
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
