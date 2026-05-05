package com.example.tradesystem.merchant.domain.model;

import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 商家收款记录实体
 */
@Data
@NoArgsConstructor
public class MerchantCreditRecord {
    
    private String creditId;
    private Long merchantId;
    private Money amount;
    private String orderId;
    private LocalDateTime createdAt;

    public MerchantCreditRecord(String creditId, Long merchantId, Money amount, String orderId) {
        this.creditId = creditId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }
}
