package com.example.tradesystem.trade.application.job;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.trade.domain.event.OrderFailedEvent;
import com.example.tradesystem.trade.domain.model.Order;
import com.example.tradesystem.trade.infrastructure.mapper.OrderMapper;
import com.example.tradesystem.user.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderTimeoutJob 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单超时检查任务测试")
class OrderTimeoutJobTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderTimeoutJob orderTimeoutJob;

    private LocalDateTime now;
    private LocalDateTime timeoutThreshold;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        timeoutThreshold = now.minusMinutes(30); // 默认超时时间30分钟
        
        // 设置配置参数
        ReflectionTestUtils.setField(orderTimeoutJob, "timeoutMinutes", 30);
        ReflectionTestUtils.setField(orderTimeoutJob, "scanLimit", 100);
    }

    @Test
    @DisplayName("测试超时订单检查 - 无超时订单")
    void testCheckTimeoutOrders_NoTimeoutOrders() {
        // Given
        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(Collections.emptyList());

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        verify(orderMapper, times(1)).selectTimeoutOrders(
            eq(OrderStatus.CREATED), 
            any(LocalDateTime.class), 
            eq(100)
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("测试超时订单检查 - 有超时订单，正常处理")
    void testCheckTimeoutOrders_HasTimeoutOrders_Success() {
        // Given
        Order order1 = createOrder("ORDER_001", 1001L, 2001L, now.minusMinutes(35));
        Order order2 = createOrder("ORDER_002", 1002L, 2002L, now.minusMinutes(40));
        List<Order> timeoutOrders = new ArrayList<>();
        timeoutOrders.add(order1);
        timeoutOrders.add(order2);

        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(timeoutOrders);
        when(orderMapper.updateStatus(anyString(), any(OrderStatus.class), any(), anyString()))
                .thenReturn(1);

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        verify(orderMapper, times(1)).selectTimeoutOrders(
            eq(OrderStatus.CREATED), 
            any(LocalDateTime.class), 
            eq(100)
        );
        verify(orderMapper, times(2)).updateStatus(
            anyString(), 
            eq(OrderStatus.FAILED), 
            isNull(), 
            contains("订单处理超时")
        );
        verify(eventPublisher, times(2)).publishEvent(any(OrderFailedEvent.class));
    }

    @Test
    @DisplayName("测试超时订单检查 - 订单状态更新失败")
    void testCheckTimeoutOrders_UpdateStatusFailed() {
        // Given
        Order order = createOrder("ORDER_003", 1001L, 2001L, now.minusMinutes(35));
        List<Order> timeoutOrders = Collections.singletonList(order);

        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(timeoutOrders);
        when(orderMapper.updateStatus(anyString(), any(OrderStatus.class), any(), anyString()))
                .thenReturn(0); // 更新失败

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        verify(orderMapper, times(1)).updateStatus(
            anyString(), 
            eq(OrderStatus.FAILED), 
            isNull(), 
            contains("订单处理超时")
        );
        // 更新失败时不应发布事件
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("测试超时订单检查 - 单个订单处理异常不影响其他订单")
    void testCheckTimeoutOrders_SingleOrderException() {
        // Given
        Order order1 = createOrder("ORDER_004", 1001L, 2001L, now.minusMinutes(35));
        Order order2 = createOrder("ORDER_005", 1002L, 2002L, now.minusMinutes(40));
        List<Order> timeoutOrders = new ArrayList<>();
        timeoutOrders.add(order1);
        timeoutOrders.add(order2);

        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(timeoutOrders);
        // 第一个订单处理成功
        when(orderMapper.updateStatus(eq("ORDER_004"), any(OrderStatus.class), any(), anyString()))
                .thenReturn(1);
        // 第二个订单处理时抛出异常
        when(orderMapper.updateStatus(eq("ORDER_005"), any(OrderStatus.class), any(), anyString()))
                .thenThrow(new RuntimeException("数据库异常"));

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> {
            orderTimeoutJob.checkTimeoutOrders();
        });

        // 验证两个订单都被尝试处理
        verify(orderMapper, times(2)).updateStatus(
            anyString(), 
            eq(OrderStatus.FAILED), 
            isNull(), 
            contains("订单处理超时")
        );
        // 只有第一个订单发布了事件
        verify(eventPublisher, times(1)).publishEvent(any(OrderFailedEvent.class));
    }

    @Test
    @DisplayName("测试超时订单检查 - 验证发布的失败事件内容")
    void testCheckTimeoutOrders_EventContent() {
        // Given
        Order order = createOrder("ORDER_006", 1001L, 2001L, now.minusMinutes(35));
        List<Order> timeoutOrders = Collections.singletonList(order);

        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(timeoutOrders);
        when(orderMapper.updateStatus(anyString(), any(OrderStatus.class), any(), anyString()))
                .thenReturn(1);

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        ArgumentCaptor<OrderFailedEvent> eventCaptor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        OrderFailedEvent event = eventCaptor.getValue();
        assertEquals("ORDER_006", event.getOrderId());
        assertEquals("ORDER_FAILED", event.getEventType());
        assertTrue(event.getReason().contains("订单处理超时"));
        assertEquals(OrderFailedEvent.FailedStage.BALANCE_DEDUCTION, event.getFailedStage());
    }

    @Test
    @DisplayName("测试超时订单检查 - 扫描任务整体异常不中断")
    void testCheckTimeoutOrders_OverallException() {
        // Given
        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> {
            orderTimeoutJob.checkTimeoutOrders();
        });
    }

    @Test
    @DisplayName("测试超时订单检查 - 验证超时阈值计算正确")
    void testCheckTimeoutOrders_TimeoutThresholdCalculation() {
        // Given
        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(Collections.emptyList());

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderMapper, times(1)).selectTimeoutOrders(
            eq(OrderStatus.CREATED),
            thresholdCaptor.capture(),
            eq(100)
        );

        LocalDateTime capturedThreshold = thresholdCaptor.getValue();
        assertNotNull(capturedThreshold);
        // 验证阈值在预期范围内（允许1秒误差）
        long expectedDiff = 30 * 60; // 30分钟
        long actualDiff = java.time.Duration.between(capturedThreshold, now).getSeconds();
        assertTrue(Math.abs(actualDiff - expectedDiff) <= 1);
    }

    @Test
    @DisplayName("测试超时订单检查 - 自定义超时时间配置")
    void testCheckTimeoutOrders_CustomTimeoutMinutes() {
        // Given
        ReflectionTestUtils.setField(orderTimeoutJob, "timeoutMinutes", 60); // 设置为60分钟
        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(Collections.emptyList());

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderMapper, times(1)).selectTimeoutOrders(
            eq(OrderStatus.CREATED),
            thresholdCaptor.capture(),
            eq(100)
        );

        LocalDateTime capturedThreshold = thresholdCaptor.getValue();
        // 验证阈值为60分钟前
        long expectedDiff = 60 * 60; // 60分钟
        long actualDiff = java.time.Duration.between(capturedThreshold, now).getSeconds();
        assertTrue(Math.abs(actualDiff - expectedDiff) <= 1);
    }

    @Test
    @DisplayName("测试超时订单检查 - 自定义扫描数量限制")
    void testCheckTimeoutOrders_CustomScanLimit() {
        // Given
        ReflectionTestUtils.setField(orderTimeoutJob, "scanLimit", 50);
        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(50)))
                .thenReturn(Collections.emptyList());

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        verify(orderMapper, times(1)).selectTimeoutOrders(
            eq(OrderStatus.CREATED),
            any(LocalDateTime.class),
            eq(50)
        );
    }

    @Test
    @DisplayName("测试超时订单检查 - 大量超时订单分批处理")
    void testCheckTimeoutOrders_ManyTimeoutOrders() {
        // Given
        List<Order> timeoutOrders = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            timeoutOrders.add(createOrder("ORDER_" + String.format("%03d", i), 
                1000L + i, 2001L, now.minusMinutes(35 + i)));
        }

        when(orderMapper.selectTimeoutOrders(eq(OrderStatus.CREATED), any(LocalDateTime.class), eq(100)))
                .thenReturn(timeoutOrders);
        when(orderMapper.updateStatus(anyString(), any(OrderStatus.class), any(), anyString()))
                .thenReturn(1);

        // When
        orderTimeoutJob.checkTimeoutOrders();

        // Then
        verify(orderMapper, times(100)).updateStatus(
            anyString(), 
            eq(OrderStatus.FAILED), 
            isNull(), 
            contains("订单处理超时")
        );
        verify(eventPublisher, times(100)).publishEvent(any(OrderFailedEvent.class));
    }

    /**
     * 创建测试订单
     */
    private Order createOrder(String orderId, Long userId, Long merchantId, LocalDateTime createdAt) {
        Order order = new Order(orderId, userId, merchantId, new Money(new BigDecimal("1000.00")));
        // 使用反射设置createdAt，因为构造函数中会自动设置为now
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        return order;
    }
}
