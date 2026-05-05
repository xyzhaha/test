package com.example.tradesystem.trade.infrastructure.mapper;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.trade.domain.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    int insert(Order order);

    /**
     * 根据订单ID查询
     */
    Order selectById(@Param("orderId") String orderId);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("orderId") String orderId, 
                     @Param("status") OrderStatus status,
                     @Param("completedAt") java.time.LocalDateTime completedAt,
                     @Param("failureReason") String failureReason);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> selectByUserId(@Param("userId") Long userId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /**
     * 根据商家ID查询订单列表
     */
    List<Order> selectByMerchantId(@Param("merchantId") Long merchantId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计用户订单总数
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 统计商家订单总数
     */
    long countByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 查询超时订单（创建时间超过指定阈值且状态仍为CREATED的订单）
     */
    List<Order> selectTimeoutOrders(@Param("status") OrderStatus status,
                                     @Param("timeoutThreshold") java.time.LocalDateTime timeoutThreshold,
                                     @Param("limit") int limit);
}
