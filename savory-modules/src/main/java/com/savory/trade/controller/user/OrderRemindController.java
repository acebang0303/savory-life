package com.savory.trade.controller.user;

import com.savory.common.result.Result;
import com.savory.trade.websocket.WebSocketServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson2.JSON;
import java.util.Map;

/**
 * C端催单接口
 */
@RestController
@RequestMapping("/user/order")
@Slf4j
@Tag(name = "催单相关接口")
public class OrderRemindController {

    @PutMapping("/{id}/remind")
    @Operation(summary = "催单（通过WebSocket推送给商家）")
    public Result<String> remind(@PathVariable Long id) {
        log.info("用户催单: orderId={}", id);

        // 通过WebSocket通知商家
        String message = JSON.toJSONString(Map.of(
                "type", "remind",
                "orderId", id,
                "message", "用户催单，请尽快处理"
        ));
        WebSocketServer.broadcast(message);

        return Result.success();
    }
}
