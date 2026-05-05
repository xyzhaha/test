package com.example.tradesystem.merchant.infrastructure.mapper;

import com.example.tradesystem.merchant.domain.model.SettlementRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对账记录Mapper
 */
@Mapper
public interface SettlementRecordMapper {

    /**
     * 插入对账记录
     */
    int insert(SettlementRecord settlementRecord);

    /**
     * 根据商家ID和日期查询
     */
    SettlementRecord selectByMerchantAndDate(@Param("merchantId") Long merchantId,
                                              @Param("settlementDate") String settlementDate);

    /**
     * 查询商家的对账记录列表
     */
    List<SettlementRecord> selectByMerchantId(@Param("merchantId") Long merchantId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    /**
     * 统计商家的对账记录总数
     */
    long countByMerchantId(@Param("merchantId") Long merchantId);
}
