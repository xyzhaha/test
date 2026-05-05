package com.example.tradesystem.merchant.domain.event;

import com.example.tradesystem.common.event.DomainEvent;
import lombok.Getter;

/**
 * 库存扣减事件
 */
@Getter
public class InventoryDeductedEvent extends DomainEvent {
    
    private final String orderId;
    private final Long merchantId;
    private final String sku;
    private final Integer deductedQuantity;
    private final String creditRecordId;

    public InventoryDeductedEvent(String orderId, Long merchantId, String sku,
                                  Integer deductedQuantity, String creditRecordId) {
        super(orderId, "INVENTORY_DEDUCTED");
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.sku = sku;
        this.deductedQuantity = deductedQuantity;
        this.creditRecordId = creditRecordId;
    }
}
