package com.example.tradesystem.merchant.infrastructure.mapper;

import com.example.tradesystem.merchant.domain.model.MerchantCreditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家收款记录Mapper
 */
@Mapper
public interface MerchantCreditRecordMapper {

    /**
     * 插入收款记录
     */
    int insert(MerchantCreditRecord creditRecord);

    /**
     * 根据订单ID查询
     */
    MerchantCreditRecord selectByOrderId(@Param("orderId") String orderId);

    /**
     * 统计指定日期范围内的收款总额
     */
    java.math.BigDecimal sumAmountByDateRange(@Param("merchantId") Long merchantId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    /**
     * 查询商家的收款记录列表
     */
    List<MerchantCreditRecord> selectByMerchantId(@Param("merchantId") Long merchantId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);
}
