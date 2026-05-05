package com.example.tradesystem.user.infrastructure.mapper;

import com.example.tradesystem.user.domain.model.RechargeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 充值记录Mapper
 */
@Mapper
public interface RechargeRecordMapper {

    /**
     * 插入充值记录
     */
    int insert(RechargeRecord rechargeRecord);

    /**
     * 根据requestId查询
     */
    RechargeRecord selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据用户ID查询充值记录列表
     */
    List<RechargeRecord> selectByUserId(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 统计用户充值记录总数
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 根据订单ID查询扣款记录
     */
    RechargeRecord selectByOrderId(@Param("orderId") String orderId);
}
