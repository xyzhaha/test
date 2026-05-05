package com.example.tradesystem.merchant.infrastructure.mapper;

import com.example.tradesystem.merchant.domain.model.ProductInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品库存Mapper
 */
@Mapper
public interface ProductInventoryMapper {

    /**
     * 根据SKU查询库存(带行锁)
     */
    ProductInventory selectBySkuForUpdate(@Param("merchantId") Long merchantId, 
                                           @Param("sku") String sku);

    /**
     * 根据SKU查询库存
     */
    ProductInventory selectBySku(@Param("merchantId") Long merchantId, 
                                  @Param("sku") String sku);

    /**
     * 插入库存记录
     */
    int insert(ProductInventory inventory);

    /**
     * 更新库存(乐观锁)
     */
    int updateWithVersion(ProductInventory inventory);

    /**
     * 扣减库存(乐观锁)
     */
    int deductStockWithVersion(@Param("merchantId") Long merchantId,
                                @Param("sku") String sku,
                                @Param("quantity") Integer quantity,
                                @Param("version") Integer version);

    /**
     * 恢复库存(乐观锁)
     */
    int restoreStockWithVersion(@Param("merchantId") Long merchantId,
                                 @Param("sku") String sku,
                                 @Param("quantity") Integer quantity,
                                 @Param("version") Integer version);
}
