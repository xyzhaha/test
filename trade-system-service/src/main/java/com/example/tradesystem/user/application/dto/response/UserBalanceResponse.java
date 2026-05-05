package com.example.tradesystem.user.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 用户余额响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBalanceResponse {

    private Long userId;
    private BigDecimal balance;
    private String currency;
}
