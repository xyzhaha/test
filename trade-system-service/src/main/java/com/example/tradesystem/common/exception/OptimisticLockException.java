package com.example.tradesystem.common.exception;

/**
 * 乐观锁冲突异常
 */
public class OptimisticLockException extends BusinessException {
    
    public OptimisticLockException(String message) {
        super(500, message);
    }
}
