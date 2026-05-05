package com.example.tradesystem.common.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
