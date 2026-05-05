package com.example.tradesystem.user.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Getter;

/**
 * 用户余额扣减事件
 */
@Getter
public class UserBalanceDeductedEvent extends DomainEvent {
    
    private final String orderId;
    private final Long userId;
    private final Money deductedAmount;
    private final String deductionRecordId;

    public UserBalanceDeductedEvent(String orderId, Long userId, 
                                    Money deductedAmount, String deductionRecordId) {
        super(orderId, "USER_BALANCE_DEDUCTED");
        this.orderId = orderId;
        this.userId = userId;
        this.deductedAmount = deductedAmount;
        this.deductionRecordId = deductionRecordId;
    }
}
