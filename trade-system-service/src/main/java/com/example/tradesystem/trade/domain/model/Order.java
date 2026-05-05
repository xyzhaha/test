package com.example.tradesystem.trade.domain.model;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单聚合根
 */
@Data
@NoArgsConstructor
public class Order {
    
    private String orderId;
    private Long userId;
    private Long merchantId;
    private OrderStatus status;
    private Money totalAmount;
    private List<OrderItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String failureReason;
    private Integer version;

    public Order(String orderId, Long userId, Long merchantId, Money totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.status = OrderStatus.CREATED;
        this.totalAmount = totalAmount;
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.version = 0;
    }

    /**
     * 添加订单项
     */
    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    /**
     * 标记为完成
     */
    public void markAsCompleted() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("订单状态不是CREATED，无法标记为完成");
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String reason) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("订单状态不是CREATED，无法标记为失败");
        }
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }
}
