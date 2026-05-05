package com.example.tradesystem.common.exception;

/**
 * 库存不足异常
 */
public class InsufficientStockException extends BusinessException {
    
    public InsufficientStockException(String message) {
        super(400, message);
    }
}
