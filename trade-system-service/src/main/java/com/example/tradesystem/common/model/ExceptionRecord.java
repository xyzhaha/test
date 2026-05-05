package com.example.tradesystem.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异常记录实体
 * 用于记录需要人工介入的异常情况
 */
@Data
@NoArgsConstructor
public class ExceptionRecord {
    
    private String exceptionId;           // 异常ID（主键）
    private String businessType;          // 业务类型（如ORDER_CREATE、BALANCE_DEDUCTION）
    private String businessId;            // 业务ID（如订单ID、充值ID）
    private String errorMessage;          // 错误信息
    private String stackTrace;            // 堆栈信息（系统异常时填写）
    private String status;                // 处理状态（PENDING=待处理, RESOLVED=已解决）
    private LocalDateTime createdAt;      // 异常发生时间

    public ExceptionRecord(String exceptionId, String businessType, String businessId, 
                          String errorMessage, String stackTrace) {
        this.exceptionId = exceptionId;
        this.businessType = businessType;
        this.businessId = businessId;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }
}
