package com.example.tradesystem.trade.domain.model;

import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单项实体
 */
@Data
@NoArgsConstructor
public class OrderItem {
    
    private String orderItemId;
    private String orderId;
    private Long merchantId;
    private String sku;
    private String productName;
    private Integer quantity;
    private Money unitPrice;
    private Money totalPrice;

    public OrderItem(String orderItemId, String orderId, Long merchantId, String sku, String productName, 
                     Integer quantity, Money unitPrice) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(quantity);
    }
}
