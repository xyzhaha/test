package com.example.tradesystem.trade.infrastructure.mapper;

import com.example.tradesystem.trade.domain.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项Mapper
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入订单项
     */
    int batchInsert(@Param("items") List<OrderItem> items);

    /**
     * 根据订单ID查询订单项列表
     */
    List<OrderItem> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 插入单个订单项
     */
    int insert(OrderItem orderItem);
}
