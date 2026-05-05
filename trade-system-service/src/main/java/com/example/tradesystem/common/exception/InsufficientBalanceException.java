package com.example.tradesystem.common.exception;

/**
 * 余额不足异常
 */
public class InsufficientBalanceException extends BusinessException {
    
    public InsufficientBalanceException(String message) {
        super(400, message);
    }
}
