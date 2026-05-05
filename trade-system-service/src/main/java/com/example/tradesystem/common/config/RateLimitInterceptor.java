package com.example.tradesystem.common.config;

import com.example.tradesystem.common.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 限流拦截器
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiterManager rateLimiterManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            // 没有注解,不限流
            return true;
        }

        // 构建限流key
        String key = buildRateLimitKey(request, rateLimit);
        
        // 尝试获取许可
        boolean acquired = rateLimiterManager.tryAcquire(key, rateLimit.permitsPerSecond());

        if (!acquired) {
            log.warn("请求被限流: key={}, uri={}", key, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁,请稍后重试\",\"data\":null}");
            } catch (Exception e) {
                log.error("写入响应失败", e);
            }
            return false;
        }

        return true;
    }

    /**
     * 构建限流key
     */
    private String buildRateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        String baseKey = request.getRequestURI();

        switch (rateLimit.limitType()) {
            case API:
                // 接口级限流: URI作为key
                return "rate_limit:api:" + baseKey;
            
            case USER:
                // 用户级限流: URI + 用户ID作为key
                String userId = request.getParameter("userId");
                if (userId == null || userId.isEmpty()) {
                    // 从路径变量中获取
                    userId = extractUserIdFromPath(request);
                }
                if (userId != null && !userId.isEmpty()) {
                    return "rate_limit:user:" + userId + ":" + baseKey;
                }
                // 如果没有用户ID,降级为接口级限流
                return "rate_limit:api:" + baseKey;
            
            default:
                return "rate_limit:api:" + baseKey;
        }
    }

    /**
     * 从路径中提取userId
     */
    private String extractUserIdFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 例如: /api/v1/users/1001/accounts -> 提取1001
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("users".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
