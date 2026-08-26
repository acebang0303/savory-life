package com.savory.market.shortlink.controller;

import com.savory.common.exception.OrderBusinessException;
import com.savory.common.result.Result;
import com.savory.market.shortlink.component.CreateRateLimiter;
import com.savory.market.shortlink.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链生成接口。
 */
@RestController
@RequestMapping("/api/short-link")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final CreateRateLimiter rateLimiter;

    public ShortLinkController(ShortLinkService shortLinkService, CreateRateLimiter rateLimiter) {
        this.shortLinkService = shortLinkService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/create")
    public Result<String> create(@RequestParam String longUrl, HttpServletRequest request) {
        String ip = resolveIp(request);
        if (!rateLimiter.tryAcquire(ip)) {
            throw new OrderBusinessException("请求过于频繁，请稍后再试");
        }
        return Result.success(shortLinkService.create(longUrl));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
