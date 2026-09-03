package com.savory.trade.service.impl;

import com.savory.market.service.CouponService;
import com.savory.market.service.SeckillService;
import com.savory.pojo.entity.Orders;
import com.savory.trade.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final SeckillService seckillService = mock(SeckillService.class);
    private final CouponService couponService = mock(CouponService.class);

    private OrderServiceImpl newService() {
        OrderServiceImpl svc = new OrderServiceImpl();
        ReflectionTestUtils.setField(svc, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(svc, "seckillService", seckillService);
        ReflectionTestUtils.setField(svc, "couponService", couponService);
        return svc;
    }

    @Test
    void timeoutShouldNotCancelAlreadyPaidOrder() {
        // CAS 返回 0 = 状态已变更（已支付），应跳过取消和回补
        when(orderMapper.cancelPendingIfUnpaid(any())).thenReturn(0);
        OrderServiceImpl svc = newService();

        svc.handleTimeoutOrder(100L);

        verify(orderMapper).cancelPendingIfUnpaid(100L);
        verify(seckillService, never()).restoreSeckillOnTimeout(any(), any());
        verify(couponService, never()).release(any());
    }

    @Test
    void timeoutShouldCancelAndRestockWhenStillPending() {
        // CAS 返回 1 = 成功取消，需回补秒杀库存
        Orders order = new Orders();
        order.setId(100L);
        order.setIsSeckill(1);
        order.setSeckillActivityId(5L);
        order.setUserId(404L);
        when(orderMapper.cancelPendingIfUnpaid(100L)).thenReturn(1);
        when(orderMapper.selectById(100L)).thenReturn(order);
        OrderServiceImpl svc = newService();

        svc.handleTimeoutOrder(100L);

        verify(seckillService).restoreSeckillOnTimeout(5L, 404L);
    }
}
