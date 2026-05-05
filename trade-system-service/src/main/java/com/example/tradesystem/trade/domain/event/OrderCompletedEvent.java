package com.example.tradesystem.trade.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 订单完成事件
 */
@Getter
public class OrderCompletedEvent extends DomainEvent {
    
    private final String orderId;
    private final LocalDateTime completeTime;

    public OrderCompletedEvent(String orderId, LocalDateTime completeTime) {
        super(orderId, "ORDER_COMPLETED");
        this.orderId = orderId;
        this.completeTime = completeTime;
    }
}
