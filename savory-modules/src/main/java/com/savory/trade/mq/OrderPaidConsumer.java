package com.savory.trade.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.savory.pojo.entity.Orders;
import com.savory.trade.mapper.OrderMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 支付成功消息消费者：消费 order-paid-topic，回写 orders.pay_status/status。
 */
@Component
@Slf4j
public class OrderPaidConsumer {

    private static final String TOPIC = "order-paid-topic";

    @Value("${rocketmq.name-server:localhost:9876}")
    private String nameServer;

    @Value("${rocketmq.consumer.group:order-paid-consumer}")
    private String consumerGroup;

    @Autowired
    private OrderMapper orderMapper;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void start() throws MQClientException {
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(TOPIC, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, ctx) -> {
            for (MessageExt msg : msgs) {
                String orderNo = new String(msg.getBody(), StandardCharsets.UTF_8);
                try {
                    Orders order = orderMapper.selectOne(
                            new LambdaQueryWrapper<Orders>().eq(Orders::getNumber, orderNo));
                    if (order != null && order.getPayStatus().equals(Orders.UN_PAID)) {
                        order.setPayStatus(Orders.PAID);
                        order.setStatus(Orders.TO_BE_CONFIRMED);
                        order.setPayTime(LocalDateTime.now());
                        orderMapper.updateById(order);
                    }
                } catch (Exception e) {
                    log.warn("回写订单支付状态失败 orderNo={}: {}", orderNo, e.getMessage());
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("OrderPaidConsumer 启动成功, topic={}", TOPIC);
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
