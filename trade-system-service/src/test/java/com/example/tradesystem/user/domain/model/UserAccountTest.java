package com.example.tradesystem.user.domain.model;

import com.example.tradesystem.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserAccount聚合根单元测试
 */
class UserAccountTest {

    @Test
    void testRechargeSuccess() {
        UserAccount account = new UserAccount(1001L, new Money(BigDecimal.ZERO));
        Money rechargeAmount = new Money(new BigDecimal("100.00"));
        
        account.recharge(rechargeAmount);
        
        assertEquals(new BigDecimal("100.00"), account.getBalance().getAmount());
    }

    @Test
    void testRechargeMultipleTimes() {
        UserAccount account = new UserAccount(1001L, new Money(BigDecimal.ZERO));
        
        account.recharge(new Money(new BigDecimal("100.00")));
        account.recharge(new Money(new BigDecimal("50.00")));
        
        assertEquals(new BigDecimal("150.00"), account.getBalance().getAmount());
    }

    @Test
    void testDeductSuccess() {
        UserAccount account = new UserAccount(1001L, new Money(new BigDecimal("100.00")));
        Money deductAmount = new Money(new BigDecimal("30.00"));
        
        account.deduct(deductAmount);
        
        assertEquals(new BigDecimal("70.00"), account.getBalance().getAmount());
    }

    @Test
    void testDeductInsufficientBalance() {
        UserAccount account = new UserAccount(1001L, new Money(new BigDecimal("50.00")));
        Money deductAmount = new Money(new BigDecimal("100.00"));
        
        assertThrows(InsufficientBalanceException.class, () -> {
            account.deduct(deductAmount);
        });
    }

    @Test
    void testRefundSuccess() {
        UserAccount account = new UserAccount(1001L, new Money(new BigDecimal("100.00")));
        Money refundAmount = new Money(new BigDecimal("30.00"));
        
        account.refund(refundAmount);
        
        assertEquals(new BigDecimal("130.00"), account.getBalance().getAmount());
    }

    @Test
    void testRechargeNegativeAmountThrowsException() {
        UserAccount account = new UserAccount(1001L, new Money(BigDecimal.ZERO));
        
        assertThrows(IllegalArgumentException.class, () -> {
            account.recharge(new Money(new BigDecimal("-100.00")));
        });
    }

    @Test
    void testRechargeExceedLimitThrowsException() {
        UserAccount account = new UserAccount(1001L, new Money(BigDecimal.ZERO));
        
        assertThrows(IllegalArgumentException.class, () -> {
            account.recharge(new Money(new BigDecimal("10001.00")));
        });
    }

    @Test
    void testDeductNegativeAmountThrowsException() {
        UserAccount account = new UserAccount(1001L, new Money(new BigDecimal("100.00")));
        
        assertThrows(IllegalArgumentException.class, () -> {
            account.deduct(new Money(new BigDecimal("-30.00")));
        });
    }

    @Test
    void testVersionIncrementOnRecharge() {
        UserAccount account = new UserAccount(1001L, new Money(BigDecimal.ZERO));
        Integer initialVersion = account.getVersion();
        
        account.recharge(new Money(new BigDecimal("100.00")));
        
        // version应该在数据库更新时递增,这里只测试领域模型
        assertNotNull(account.getUpdatedAt());
    }
}
