package com.example.tradesystem.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key(支持SpEL表达式)
     */
    String key() default "";

    /**
     * 每秒允许的请求数
     */
    double permitsPerSecond() default 10.0;

    /**
     * 限流类型: API级别或用户级别
     */
    LimitType limitType() default LimitType.API;

    enum LimitType {
        API,    // 接口级限流
        USER    // 用户级限流
    }
}
