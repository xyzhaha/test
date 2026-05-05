package com.example.tradesystem.merchant.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import lombok.Getter;

/**
 * 库存恢复事件
 */
@Getter
public class StockRestoredEvent extends DomainEvent {
    
    private final String orderId;
    private final String sku;
    private final Integer restoredQuantity;

    public StockRestoredEvent(String orderId, String sku, Integer restoredQuantity) {
        super(orderId, "STOCK_RESTORED");
        this.orderId = orderId;
        this.sku = sku;
        this.restoredQuantity = restoredQuantity;
    }
}
