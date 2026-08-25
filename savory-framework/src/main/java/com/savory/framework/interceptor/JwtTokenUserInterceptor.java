package com.savory.framework.interceptor;

import com.savory.common.constant.JwtClaimsConstant;
import com.savory.common.context.BaseContext;
import com.savory.framework.properties.JwtProperties;
import com.savory.framework.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * C端用户JWT令牌拦截器
 * 验证请求头中的 Authorization，查黑名单，解析后设置 ThreadLocal
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、校验令牌是否为空
        if (token == null || token.isEmpty()) {
            log.warn("用户令牌为空，请求URI: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }

        try {
            //3、解析JWT获取tokenID
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            String jti = claims.getId();

            //4、校验token是否在黑名单中（已退出登录）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti))) {
                log.warn("用户令牌已被加入黑名单，jti: {}", jti);
                response.setStatus(401);
                return false;
            }

            //5、获取用户ID存入ThreadLocal
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            BaseContext.setCurrentId(userId);

            log.debug("用户认证通过，userId: {}", userId);
            return true;
        } catch (Exception e) {
            log.warn("用户令牌校验失败: {}", e.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //清除ThreadLocal，防止内存泄漏
        BaseContext.removeCurrentId();
    }
}
