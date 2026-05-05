package com.example.tradesystem.merchant.domain.model;

import com.example.tradesystem.common.exception.InsufficientStockException;
import com.example.tradesystem.user.domain.model.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductInventory聚合根单元测试
 */
class ProductInventoryTest {

    @Test
    void testAddStockSuccess() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        inventory.addStock(50);
        
        assertEquals(150, inventory.getStockQuantity());
    }

    @Test
    void testDeductStockSuccess() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        inventory.deductStock(30);
        
        assertEquals(70, inventory.getStockQuantity());
        assertEquals(30, inventory.getSoldQuantity());
    }

    @Test
    void testDeductStockInsufficientThrowsException() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            10
        );
        
        assertThrows(InsufficientStockException.class, () -> {
            inventory.deductStock(20);
        });
    }

    @Test
    void testRestoreStockSuccess() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        inventory.deductStock(30);
        inventory.restoreStock(10);
        
        assertEquals(80, inventory.getStockQuantity());
        assertEquals(20, inventory.getSoldQuantity());
    }

    @Test
    void testIsStockSufficient() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        assertTrue(inventory.isStockSufficient(50));
        assertTrue(inventory.isStockSufficient(100));
        assertFalse(inventory.isStockSufficient(101));
    }

    @Test
    void testUpdatePriceSuccess() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        inventory.updatePrice(new Money(new BigDecimal("3199.00")));
        
        assertEquals(new BigDecimal("3199.00"), inventory.getPrice().getAmount());
    }

    @Test
    void testAddStockNegativeQuantityThrowsException() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            inventory.addStock(-10);
        });
    }

    @Test
    void testUpdatePriceNegativeThrowsException() {
        ProductInventory inventory = new ProductInventory(
            "SKU_001", 
            2001L, 
            "iPhone 15", 
            new Money(new BigDecimal("2999.00")), 
            100
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            inventory.updatePrice(new Money(new BigDecimal("-100.00")));
        });
    }
}
