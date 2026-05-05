package com.example.tradesystem.merchant.interfaces.controller;

import com.example.tradesystem.common.response.ApiResponse;
import com.example.tradesystem.merchant.application.service.MerchantService;
import com.example.tradesystem.merchant.domain.model.MerchantAccount;
import com.example.tradesystem.merchant.domain.model.ProductInventory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 商家控制器
 */
@RestController
@RequestMapping("/api/v1/merchants")
@Slf4j
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    /**
     * 添加/更新库存
     */
    @PostMapping("/{merchantId}/products/{sku}/stock")
    public ApiResponse<ProductInventory> addStock(
            @PathVariable Long merchantId,
            @PathVariable String sku,
            @Validated @RequestBody AddStockRequest request) {
        log.info("收到添加库存请求: merchantId={}, sku={}, quantity={}", merchantId, sku, request.getQuantity());
        
        ProductInventory inventory = merchantService.addStock(
            merchantId, 
            sku, 
            request.getQuantity(), 
            request.getPrice(),
            request.getProductName()
        );
        
        return ApiResponse.success(inventory);
    }

    /**
     * 查询商品信息
     */
    @GetMapping("/{merchantId}/products/{sku}")
    public ApiResponse<ProductInventory> getProductInfo(
            @PathVariable Long merchantId,
            @PathVariable String sku) {
        log.info("查询商品信息: merchantId={}, sku={}", merchantId, sku);
        ProductInventory inventory = merchantService.getProductInfo(merchantId, sku);
        return ApiResponse.success(inventory);
    }

    /**
     * 查询商家账户
     */
    @GetMapping("/{merchantId}/accounts")
    public ApiResponse<MerchantAccount> getMerchantAccount(@PathVariable Long merchantId) {
        log.info("查询商家账户: merchantId={}", merchantId);
        MerchantAccount account = merchantService.getMerchantAccount(merchantId);
        return ApiResponse.success(account);
    }

    /**
     * 添加库存请求DTO
     */
    @Data
    public static class AddStockRequest {
        
        @NotNull(message = "库存数量不能为空")
        private Integer quantity;

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        @DecimalMax(value = "999999.99", message = "商品价格超出限制")
        private BigDecimal price;

        private String productName;
    }
}
