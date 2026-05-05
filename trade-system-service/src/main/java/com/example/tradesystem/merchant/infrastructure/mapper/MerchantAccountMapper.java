package com.example.tradesystem.merchant.infrastructure.mapper;

import com.example.tradesystem.merchant.domain.model.MerchantAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商家账户Mapper
 */
@Mapper
public interface MerchantAccountMapper {

    /**
     * 查询商家账户
     */
    MerchantAccount selectById(@Param("merchantId") Long merchantId);

    /**
     * 插入商家账户
     */
    int insert(MerchantAccount merchantAccount);

    /**
     * 增加余额(乐观锁)
     */
    int increaseBalanceWithVersion(@Param("merchantId") Long merchantId,
                                    @Param("amount") java.math.BigDecimal amount,
                                    @Param("version") Integer version);

    /**
     * 扣减余额(乐观锁)
     */
    int decreaseBalanceWithVersion(@Param("merchantId") Long merchantId,
                                    @Param("amount") java.math.BigDecimal amount,
                                    @Param("version") Integer version);
}
