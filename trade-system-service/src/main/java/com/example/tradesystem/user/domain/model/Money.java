package com.example.tradesystem.user.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 金额值对象
 */
@Data
public class Money implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final BigDecimal amount;
    private final String currency;

    /**
     * 私有构造函数，允许负数（用于内部使用，如退款）
     */
    private Money(BigDecimal amount, String currency, boolean allowNegative) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为null");
        }
        if (!allowNegative && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency != null ? currency : "CNY";
    }

    public Money(BigDecimal amount, String currency) {
        this(amount, currency, false); // 默认不允许负数
    }

    public Money(BigDecimal amount) {
        this(amount, "CNY", false); // 默认不允许负数
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.getCurrency())) {
            throw new IllegalArgumentException("货币类型不一致");
        }
        return new Money(this.amount.add(other.getAmount()), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.getCurrency())) {
            throw new IllegalArgumentException("货币类型不一致");
        }
        return new Money(this.amount.subtract(other.getAmount()), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(new BigDecimal(multiplier)), this.currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public int compareTo(Money other) {
        return this.amount.compareTo(other.getAmount());
    }

    /**
     * 取反（用于退款等场景）
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency, true); // 允许负数
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
