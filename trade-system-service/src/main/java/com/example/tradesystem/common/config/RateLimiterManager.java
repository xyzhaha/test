package com.example.tradesystem.common.config;

import com.example.tradesystem.common.annotation.RateLimit;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器管理器
 */
@Component
@Slf4j
public class RateLimiterManager {

    private final Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建限流器
     */
    public RateLimiter getOrCreate(String key, double permitsPerSecond) {
        return rateLimiterMap.computeIfAbsent(key, k -> {
            RateLimiter rateLimiter = RateLimiter.create(permitsPerSecond);
            log.info("创建限流器: key={}, permitsPerSecond={}", key, permitsPerSecond);
            return rateLimiter;
        });
    }

    /**
     * 尝试获取许可
     */
    public boolean tryAcquire(String key, double permitsPerSecond) {
        RateLimiter rateLimiter = getOrCreate(key, permitsPerSecond);
        return rateLimiter.tryAcquire();
    }
}
