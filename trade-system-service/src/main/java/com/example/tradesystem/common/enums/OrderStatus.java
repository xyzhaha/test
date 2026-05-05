package com.example.tradesystem.common.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    CREATED("CREATED", "已创建"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
