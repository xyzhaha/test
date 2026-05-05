package com.example.tradesystem.merchant.domain.model;

import com.example.tradesystem.common.enums.SettlementStatus;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录实体
 */
@Data
@NoArgsConstructor
public class SettlementRecord {
    
    private String settlementId;
    private Long merchantId;
    private LocalDate settlementDate;
    private Money totalSales;
    private Money totalReceived;
    private Money difference;
    private SettlementStatus status;
    private LocalDateTime createdAt;

    public SettlementRecord(String settlementId, Long merchantId, LocalDate settlementDate,
                           Money totalSales, Money totalReceived) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.settlementDate = settlementDate;
        this.totalSales = totalSales;
        this.totalReceived = totalReceived;
        this.difference = new Money(totalSales.getAmount().subtract(totalReceived.getAmount()));
        this.status = this.difference.isZero() ? SettlementStatus.MATCHED : SettlementStatus.MISMATCHED;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 判断是否对账一致
     */
    public boolean isMatched() {
        return this.difference != null && this.difference.isZero();
    }
}
