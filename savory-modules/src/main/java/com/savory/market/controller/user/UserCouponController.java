package com.savory.market.controller.user;

import com.savory.common.result.PageResult;
import com.savory.common.result.Result;
import com.savory.market.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * C端优惠券接口
 */
@RestController
@RequestMapping("/user/coupon")
@Slf4j
@Tag(name = "用户优惠券相关接口")
public class UserCouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/receive/{templateId}")
    @Operation(summary = "领取优惠券")
    public Result<String> receive(@PathVariable Long templateId) {
        log.info("用户领取优惠券: {}", templateId);
        couponService.receive(templateId);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "我的优惠券列表")
    public Result<PageResult> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询优惠券列表");
        return Result.success(couponService.list(page, pageSize));
    }
}
