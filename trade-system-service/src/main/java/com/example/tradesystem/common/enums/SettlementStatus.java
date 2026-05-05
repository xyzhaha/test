package com.example.tradesystem.common.enums;

/**
 * 对账状态枚举
 */
public enum SettlementStatus {
    MATCHED("MATCHED", "一致"),
    MISMATCHED("MISMATCHED", "不一致");

    private final String code;
    private final String description;

    SettlementStatus(String code, String description) {
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
