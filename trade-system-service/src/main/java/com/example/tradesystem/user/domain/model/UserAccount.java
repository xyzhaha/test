package com.example.tradesystem.user.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户账户聚合根
 */
@Data
@NoArgsConstructor
public class UserAccount {
    
    private Long userId;
    private Money balance;
    private String currency;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserAccount(Long userId, Money balance) {
        this.userId = userId;
        this.balance = balance;
        this.currency = balance.getCurrency();
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 充值
     */
    public void recharge(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }
        if (amount.getAmount().compareTo(new java.math.BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("充值金额不能超过10000元");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 扣款
     */
    public void deduct(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("扣款金额必须大于0");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new com.example.tradesystem.common.exception.InsufficientBalanceException("余额不足");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 退款
     */
    public void refund(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
}
