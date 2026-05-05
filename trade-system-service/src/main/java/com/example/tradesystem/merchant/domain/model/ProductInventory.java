package com.example.tradesystem.merchant.domain.model;

import com.example.tradesystem.common.exception.InsufficientStockException;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 商品库存聚合根
 */
@Data
@NoArgsConstructor
public class ProductInventory {
    
    private Long id;
    private String sku;
    private Long merchantId;
    private String productName;
    private Money price;
    private Integer stockQuantity;
    private Integer soldQuantity;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductInventory(String sku, Long merchantId, String productName, Money price, Integer stockQuantity) {
        this.sku = sku;
        this.merchantId = merchantId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.soldQuantity = 0;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加库存
     */
    public void addStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("库存数量必须大于0");
        }
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 扣减库存
     */
    public void deductStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于0");
        }
        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException("库存不足，当前库存: " + this.stockQuantity);
        }
        this.stockQuantity -= quantity;
        this.soldQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 恢复库存
     */
    public void restoreStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("恢复数量必须大于0");
        }
        this.stockQuantity += quantity;
        this.soldQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查库存是否充足
     */
    public boolean isStockSufficient(Integer quantity) {
        return this.stockQuantity != null && this.stockQuantity >= quantity;
    }

    /**
     * 更新价格
     */
    public void updatePrice(Money newPrice) {
        if (newPrice == null || !newPrice.isPositive()) {
            throw new IllegalArgumentException("价格必须大于0");
        }
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }
}
