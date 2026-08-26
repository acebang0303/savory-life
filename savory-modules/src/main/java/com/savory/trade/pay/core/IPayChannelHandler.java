package com.savory.trade.pay.core;

import com.savory.pojo.entity.PayChannel;
import com.savory.pojo.entity.PayOrder;
import com.savory.trade.pay.core.model.PayNotifyResult;
import com.savory.trade.pay.core.model.PayResult;
import com.savory.trade.pay.core.model.RefundResult;

import java.util.Map;

/**
 * 支付渠道策略接口：渠道差异收敛在各实现内。
 */
public interface IPayChannelHandler {

    String getChannelCode();

    /** 统一下单 */
    PayResult unifiedOrder(PayOrder order, PayChannel channel);

    /** 主动查单 */
    PayResult queryOrder(PayOrder order, PayChannel channel);

    /** 解析 + 验签回调通知 */
    PayNotifyResult parseNotify(Map<String, String> params, PayChannel channel);

    /** 关闭订单 */
    boolean closeOrder(PayOrder order, PayChannel channel);

    /** 退款 */
    RefundResult refund(String refundNo, String refundAmount, String reason,
                        PayOrder order, PayChannel channel);

    /** 渠道要求的成功应答体 */
    String notifySuccessBody();

    /** 渠道要求的失败应答体 */
    String notifyFailBody();
}
