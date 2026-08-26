package com.savory.trade.pay.channel;

import com.savory.pojo.entity.PayChannel;
import com.savory.pojo.entity.PayOrder;
import com.savory.trade.pay.core.IPayChannelHandler;
import com.savory.trade.pay.core.model.PayNotifyResult;
import com.savory.trade.pay.core.model.PayResult;
import com.savory.trade.pay.core.model.RefundResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信支付 V3 渠道（骨架预留，RSA 验签 + AES-GCM 解密待接入）。
 */
@Component
public class WechatChannelHandler implements IPayChannelHandler {

    @Override
    public String getChannelCode() {
        return "wechat";
    }

    @Override
    public PayResult unifiedOrder(PayOrder order, PayChannel channel) {
        return PayResult.fail("微信支付未配置（骨架预留）");
    }

    @Override
    public PayResult queryOrder(PayOrder order, PayChannel channel) {
        return PayResult.fail("微信支付未配置（骨架预留）");
    }

    @Override
    public PayNotifyResult parseNotify(Map<String, String> params, PayChannel channel) {
        throw new UnsupportedOperationException("微信 V3 验签骨架，待接入");
    }

    @Override
    public boolean closeOrder(PayOrder order, PayChannel channel) {
        return false;
    }

    @Override
    public RefundResult refund(String refundNo, String refundAmount, String reason,
                               PayOrder order, PayChannel channel) {
        return new RefundResult(false, "微信退款未配置（骨架预留）");
    }

    @Override
    public String notifySuccessBody() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }

    @Override
    public String notifyFailBody() {
        return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
    }
}
