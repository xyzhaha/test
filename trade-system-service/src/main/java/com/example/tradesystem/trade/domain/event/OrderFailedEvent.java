package com.example.tradesystem.trade.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import lombok.Getter;

/**
 * 订单失败事件
 */
@Getter
public class OrderFailedEvent extends DomainEvent {
    
    public enum FailedStage {
        BALANCE_DEDUCTION,      // 余额扣减失败
        INVENTORY_DEDUCTION,    // 库存扣减失败
        MERCHANT_CREDIT         // 商家收款失败
    }

    private final String orderId;
    private final String reason;
    private final FailedStage failedStage;

    public OrderFailedEvent(String orderId, String reason, FailedStage failedStage) {
        super(orderId, "ORDER_FAILED");
        this.orderId = orderId;
        this.reason = reason;
        this.failedStage = failedStage;
    }
}
