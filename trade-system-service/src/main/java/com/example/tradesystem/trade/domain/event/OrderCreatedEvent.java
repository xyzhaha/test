package com.example.tradesystem.trade.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import com.example.tradesystem.trade.domain.model.OrderItem;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Getter;
import java.util.List;

/**
 * 订单创建事件
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {
    
    private final String orderId;
    private final Long userId;
    private final Long merchantId;
    private final List<OrderItem> items;
    private final Money totalAmount;

    public OrderCreatedEvent(String orderId, Long userId, Long merchantId, 
                             List<OrderItem> items, Money totalAmount) {
        super(orderId, "ORDER_CREATED");
        this.orderId = orderId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }
}
