package com.savory.trade.controller.user;

import com.alibaba.fastjson2.JSON;
import com.savory.common.result.Result;
import com.savory.trade.mq.NotifyMessageProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * C端催单接口
 */
@RestController
@RequestMapping("/user/order")
@Slf4j
@Tag(name = "催单相关接口")
public class OrderRemindController {

    @Autowired
    private NotifyMessageProducer notifyMessageProducer;

    @PutMapping("/{id}/remind")
    @Operation(summary = "催单（通过WebSocket推送给商家）")
    public Result<String> remind(@PathVariable Long id) {
        log.info("用户催单: orderId={}", id);

        String content = JSON.toJSONString(Map.of(
                "orderId", id,
                "message", "用户催单，请尽快处理"
        ));
        notifyMessageProducer.broadcast("remind", content);

        return Result.success();
    }
}
