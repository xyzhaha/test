package com.example.tradesystem.user.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Money值对象单元测试
 */
class MoneyTest {

    @Test
    void testCreateMoney() {
        Money money = new Money(new BigDecimal("100.50"));
        assertEquals(new BigDecimal("100.50"), money.getAmount());
        assertEquals("CNY", money.getCurrency());
    }

    @Test
    void testCreateMoneyWithCurrency() {
        Money money = new Money(new BigDecimal("100.50"), "USD");
        assertEquals(new BigDecimal("100.50"), money.getAmount());
        assertEquals("USD", money.getCurrency());
    }

    @Test
    void testAddMoney() {
        Money money1 = new Money(new BigDecimal("100.00"));
        Money money2 = new Money(new BigDecimal("50.00"));
        Money result = money1.add(money2);
        assertEquals(new BigDecimal("150.00"), result.getAmount());
    }

    @Test
    void testSubtractMoney() {
        Money money1 = new Money(new BigDecimal("100.00"));
        Money money2 = new Money(new BigDecimal("30.00"));
        Money result = money1.subtract(money2);
        assertEquals(new BigDecimal("70.00"), result.getAmount());
    }

    @Test
    void testMultiplyMoney() {
        Money money = new Money(new BigDecimal("100.00"));
        Money result = money.multiply(3);
        assertEquals(new BigDecimal("300.00"), result.getAmount());
    }

    @Test
    void testIsPositive() {
        Money positiveMoney = new Money(new BigDecimal("100.00"));
        assertTrue(positiveMoney.isPositive());

        Money zeroMoney = new Money(BigDecimal.ZERO);
        assertFalse(zeroMoney.isPositive());
    }

    @Test
    void testIsZero() {
        Money zeroMoney = new Money(BigDecimal.ZERO);
        assertTrue(zeroMoney.isZero());

        Money nonZeroMoney = new Money(new BigDecimal("0.01"));
        assertFalse(nonZeroMoney.isZero());
    }

    @Test
    void testNegativeAmountThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Money(new BigDecimal("-100.00"));
        });
    }

    @Test
    void testNullAmountThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Money(null);
        });
    }

    @Test
    void testCompareTo() {
        Money money1 = new Money(new BigDecimal("100.00"));
        Money money2 = new Money(new BigDecimal("200.00"));
        Money money3 = new Money(new BigDecimal("100.00"));

        assertTrue(money1.compareTo(money2) < 0);
        assertTrue(money2.compareTo(money1) > 0);
        assertEquals(0, money1.compareTo(money3));
    }
}
