package com.example.tradesystem.common.enums;

/**
 * 充值状态枚举
 */
public enum RechargeStatus {
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    REFUNDED("REFUNDED", "已退款");

    private final String code;
    private final String description;

    RechargeStatus(String code, String description) {
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
