package com.savory.trade.controller.user;

import com.savory.common.result.PageResult;
import com.savory.common.result.Result;
import com.savory.pojo.entity.Orders;
import com.savory.trade.dto.OrderSubmitDTO;
import com.savory.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * C端订单接口
 */
@RestController
@RequestMapping("/user/order")
@Slf4j
@Tag(name = "用户订单相关接口")
public class UserOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户提交订单
     */
    @PostMapping("/submit")
    @Operation(summary = "提交订单")
    public Result<Orders> submit(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        log.info("用户提交订单: {}", orderSubmitDTO);
        Orders order = orderService.submit(orderSubmitDTO);
        return Result.success(order);
    }

    /**
     * 用户取消订单
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<String> cancel(@PathVariable Long id) {
        log.info("用户取消订单，orderId: {}", id);
        orderService.cancel(id, com.savory.common.context.BaseContext.getCurrentId());
        return Result.success();
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    @Operation(summary = "发起支付")
    public Result<String> pay(@RequestParam Long orderId) {
        log.info("用户支付订单，orderId: {}", orderId);
        orderService.pay(orderId, com.savory.common.context.BaseContext.getCurrentId());
        return Result.success();
    }

    /**
     * 历史订单查询
     */
    @GetMapping("/page")
    @Operation(summary = "历史订单查询")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) Integer status) {
        log.info("查询订单，page: {}, pageSize: {}, status: {}", page, pageSize, status);
        PageResult pageResult = orderService.pageQuery(page, pageSize, status);
        return Result.success(pageResult);
    }
}
