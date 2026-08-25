package com.savory.user.controller;

import com.savory.common.context.BaseContext;
import com.savory.common.result.Result;
import com.savory.pojo.entity.User;
import com.savory.auth.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * C端用户个人信息接口
 */
@RestController
@RequestMapping("/user/profile")
@Slf4j
@Tag(name = "用户个人信息相关接口")
public class UserProfileController {

    @Autowired
    private UserAuthService userAuthService;

    /**
     * 获取个人信息
     *
     * @return
     */
    @GetMapping
    @Operation(summary = "获取个人信息")
    public Result<User> getProfile() {
        Long userId = BaseContext.getCurrentId();
        User user = userAuthService.getUserById(userId);
        // 脱敏: 清除 openid
        if (user != null) {
            user.setOpenid(null);
        }
        return Result.success(user);
    }

    /**
     * 修改个人信息
     *
     * @param user
     * @return
     */
    @PutMapping
    @Operation(summary = "修改个人信息")
    public Result<String> updateProfile(@RequestBody User user) {
        Long userId = BaseContext.getCurrentId();
        user.setId(userId);
        user.setOpenid(null); //不更新openid
        userAuthService.updateUser(user);
        return Result.success();
    }

    /**
     * 查看成长值详情
     *
     * @return
     */
    @GetMapping("/growth")
    @Operation(summary = "查看成长值详情")
    public Result<Map<String, Object>> growth() {
        Long userId = BaseContext.getCurrentId();
        User user = userAuthService.getUserById(userId);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("growthValue", user.getGrowthValue());
            result.put("level", user.getLevel());
        }
        return Result.success(result);
    }
}
