package com.example.tradesystem.trade.interfaces.controller;

import com.example.tradesystem.common.response.ApiResponse;
import com.example.tradesystem.trade.application.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 交易控制器
 */
@RestController
@RequestMapping("/api/v1/trades")
@Slf4j
public class TradeController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/orders")
    public ApiResponse<String> createOrder(@Validated @RequestBody OrderService.CreateOrderRequest request) {
        log.info("收到创建订单请求: userId={}, merchantId={}", request.getUserId(), request.getMerchantId());
        
        String orderId = orderService.createOrder(request);
        
        return ApiResponse.success(orderId);
    }
}
