package com.example.tradesystem.user.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Getter;

/**
 * 用户余额退款事件
 */
@Getter
public class UserBalanceRefundedEvent extends DomainEvent {
    
    private final String orderId;
    private final Long userId;
    private final Money refundedAmount;

    public UserBalanceRefundedEvent(String orderId, Long userId, Money refundedAmount) {
        super(orderId, "USER_BALANCE_REFUNDED");
        this.orderId = orderId;
        this.userId = userId;
        this.refundedAmount = refundedAmount;
    }
}
