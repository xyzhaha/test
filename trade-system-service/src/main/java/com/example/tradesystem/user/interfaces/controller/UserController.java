package com.example.tradesystem.user.interfaces.controller;

import com.example.tradesystem.common.response.ApiResponse;
import com.example.tradesystem.user.application.dto.request.RechargeRequest;
import com.example.tradesystem.user.application.dto.response.RechargeResponse;
import com.example.tradesystem.user.application.dto.response.UserBalanceResponse;
import com.example.tradesystem.user.application.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 账户充值
     */
    @PostMapping("/{userId}/accounts/recharge")
    public ApiResponse<RechargeResponse> recharge(
            @PathVariable Long userId,
            @Validated @RequestBody RechargeRequest request) {
        log.info("收到充值请求: userId={}, amount={}", userId, request.getAmount());
        RechargeResponse response = userService.recharge(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 查询用户余额
     */
    @GetMapping("/{userId}/accounts")
    public ApiResponse<UserBalanceResponse> getBalance(@PathVariable Long userId) {
        log.info("查询用户余额: userId={}", userId);
        UserBalanceResponse response = userService.getBalance(userId);
        return ApiResponse.success(response);
    }
}
