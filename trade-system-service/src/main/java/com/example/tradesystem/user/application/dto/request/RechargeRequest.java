package com.example.tradesystem.user.application.dto.request;

import lombok.Data;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 充值请求DTO
 */
@Data
public class RechargeRequest {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @DecimalMax(value = "10000.00", message = "充值金额不能超过10000元")
    private BigDecimal amount;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
