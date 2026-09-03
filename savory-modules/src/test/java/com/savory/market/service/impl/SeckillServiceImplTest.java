package com.savory.market.service.impl;

import com.savory.common.context.BaseContext;
import com.savory.common.constant.MessageConstant;
import com.savory.common.exception.OrderBusinessException;
import com.savory.market.dto.SeckillBuyDTO;
import com.savory.market.mapper.SeckillActivityMapper;
import com.savory.pojo.entity.SeckillActivity;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeckillServiceImplTest {

    private final SeckillActivityMapper activityMapper = mock(SeckillActivityMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final TransactionMQProducer transactionProducer = mock(TransactionMQProducer.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private SeckillServiceImpl newService() {
        SeckillServiceImpl svc = new SeckillServiceImpl();
        ReflectionTestUtils.setField(svc, "seckillActivityMapper", activityMapper);
        ReflectionTestUtils.setField(svc, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(svc, "transactionProducer", transactionProducer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        return svc;
    }

    private SeckillActivity runningActivity() {
        SeckillActivity act = new SeckillActivity();
        act.setId(5L);
        act.setDishId(5L);
        act.setStartTime(LocalDateTime.now().minusHours(1));
        act.setEndTime(LocalDateTime.now().plusHours(1));
        act.setLimitPerUser(1);
        act.setSeckillPrice(new BigDecimal("9.90"));
        return act;
    }

    private TransactionSendResult rollbackResult() {
        // TransactionSendResult.getLocalTransactionState() 无法被 Mockito stub（嵌套 stubbing 冲突），
        // 该类有公开 setter，直接用真实对象构造。
        TransactionSendResult r = new TransactionSendResult();
        r.setLocalTransactionState(LocalTransactionState.ROLLBACK_MESSAGE);
        return r;
    }

    @AfterEach
    void cleanup() {
        BaseContext.removeCurrentId();
    }

    @Test
    void rollbackWithoutFailReasonShouldThrowStockOut() throws Exception {
        when(activityMapper.selectById(5L)).thenReturn(runningActivity());
        when(transactionProducer.sendMessageInTransaction(any(), any())).thenReturn(rollbackResult());
        when(valueOps.get(any())).thenReturn(null);  // 无失败原因标记 → 售罄
        SeckillServiceImpl svc = newService();
        BaseContext.setCurrentId(404L);
        SeckillBuyDTO dto = new SeckillBuyDTO();
        dto.setActivityId(5L);
        dto.setDishId(5L);

        assertThatThrownBy(() -> svc.seckillBuy(dto))
                .isInstanceOf(OrderBusinessException.class)
                .hasMessageContaining(MessageConstant.SECKILL_STOCK_OUT);
    }

    @Test
    void rollbackWithRepeatReasonShouldThrowRepeat() throws Exception {
        when(activityMapper.selectById(5L)).thenReturn(runningActivity());
        when(transactionProducer.sendMessageInTransaction(any(), any())).thenReturn(rollbackResult());
        when(valueOps.get(any())).thenReturn("repeat");  // 失败原因=重复秒杀
        SeckillServiceImpl svc = newService();
        BaseContext.setCurrentId(404L);
        SeckillBuyDTO dto = new SeckillBuyDTO();
        dto.setActivityId(5L);
        dto.setDishId(5L);

        assertThatThrownBy(() -> svc.seckillBuy(dto))
                .isInstanceOf(OrderBusinessException.class)
                .hasMessageContaining(MessageConstant.SECKILL_REPEAT);
    }

    @Test
    void commitStateShouldReturnOrderNo() throws Exception {
        when(activityMapper.selectById(5L)).thenReturn(runningActivity());
        TransactionSendResult commitResult = new TransactionSendResult();
        commitResult.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
        when(transactionProducer.sendMessageInTransaction(any(), any())).thenReturn(commitResult);
        SeckillServiceImpl svc = newService();
        BaseContext.setCurrentId(404L);
        SeckillBuyDTO dto = new SeckillBuyDTO();
        dto.setActivityId(5L);
        dto.setDishId(5L);

        Long orderNo = svc.seckillBuy(dto);
        // 返回值是数字串的 Long（雪花 ID），断言不为空即可
        org.assertj.core.api.Assertions.assertThat(orderNo).isNotNull();
    }
}
