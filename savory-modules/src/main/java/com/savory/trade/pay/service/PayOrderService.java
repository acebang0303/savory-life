package com.savory.trade.pay.service;

import com.savory.trade.pay.core.model.PayResult;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付编排服务。
 */
public interface PayOrderService {

    /** 处理渠道回调通知，返回渠道要求的应答体 */
    String handleNotify(String channelCode, Map<String, String> params);

    /** 统一下单：创建支付单 → 渠道下单 → 同步支付成功则入账 */
    PayResult createPayOrder(String outOrderNo, String channelCode, BigDecimal totalAmount, Long userId);
}
