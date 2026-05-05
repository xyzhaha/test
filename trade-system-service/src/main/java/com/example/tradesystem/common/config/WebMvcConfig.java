package com.example.tradesystem.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")  // 对所有API接口生效
            .excludePathPatterns(
                "/api/v1/health",        // 排除健康检查
                "/api/v1/actuator/**"    // 排除监控端点
            );
    }
}
