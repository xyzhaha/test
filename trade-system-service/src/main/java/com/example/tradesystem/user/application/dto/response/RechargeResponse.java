package com.example.tradesystem.user.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 充值响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeResponse {

    private Long userId;
    private BigDecimal balance;
    private String rechargeId;
}
