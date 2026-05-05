package com.example.tradesystem.user.domain.model;

import com.example.tradesystem.common.enums.RechargeStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 充值记录实体
 */
@Data
@NoArgsConstructor
public class RechargeRecord {
    
    private String rechargeId;
    private Long userId;
    private Money amount;
    private String requestId;
    private RechargeStatus status;
    private LocalDateTime createdAt;

    public RechargeRecord(String rechargeId, Long userId, Money amount, String requestId) {
        this.rechargeId = rechargeId;
        this.userId = userId;
        this.amount = amount;
        this.requestId = requestId;
        this.status = RechargeStatus.SUCCESS;
        this.createdAt = LocalDateTime.now();
    }
}
