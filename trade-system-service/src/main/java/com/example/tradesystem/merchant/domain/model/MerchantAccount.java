package com.example.tradesystem.merchant.domain.model;

import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 商家账户聚合根
 */
@Data
@NoArgsConstructor
public class MerchantAccount {
    
    private Long merchantId;
    private Money balance;
    private Money totalIncome;
    private String currency;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MerchantAccount(Long merchantId, Money balance) {
        this.merchantId = merchantId;
        this.balance = balance;
        this.totalIncome = balance;
        this.currency = balance.getCurrency();
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加余额(收款)
     */
    public void credit(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("收款金额必须大于0");
        }
        this.balance = this.balance.add(amount);
        this.totalIncome = this.totalIncome.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 扣减余额
     */
    public void debit(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("扣款金额必须大于0");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new com.example.tradesystem.common.exception.InsufficientBalanceException("商家余额不足");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
}
