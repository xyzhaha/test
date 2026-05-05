package com.example.tradesystem.trade.application.job;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.trade.domain.event.OrderFailedEvent;
import com.example.tradesystem.trade.domain.model.Order;
import com.example.tradesystem.trade.infrastructure.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时检查定时任务
 * 扫描超时未完成的订单，强制标记为FAILED并触发补偿机制
 */
@Component
@Slf4j
public class OrderTimeoutJob {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 订单超时时间（分钟），默认30分钟
     */
    @Value("${order.timeout.minutes:30}")
    private int timeoutMinutes;

    /**
     * 每次扫描的最大订单数量
     */
    @Value("${order.timeout.scan.limit:100}")
    private int scanLimit;

    /**
     * 定时检查超时订单
     * 默认每5分钟执行一次
     */
    @Scheduled(cron = "${order.timeout.cron:0 0/5 * * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void checkTimeoutOrders() {
        log.info("开始扫描超时订单...");

        try {
            // 计算超时阈值时间
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

            // 查询超时订单
            List<Order> timeoutOrders = orderMapper.selectTimeoutOrders(
                OrderStatus.CREATED,
                timeoutThreshold,
                scanLimit
            );

            if (timeoutOrders.isEmpty()) {
                log.info("无超时订单");
                return;
            }

            log.warn("发现{}个超时订单，开始处理...", timeoutOrders.size());

            int successCount = 0;
            int failureCount = 0;

            for (Order order : timeoutOrders) {
                try {
                    processTimeoutOrder(order);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("超时订单处理失败: orderId={}", order.getOrderId(), e);
                    // 继续处理下一个订单，不中断整个任务
                }
            }

            log.info("超时订单处理完成: 总数={}, 成功={}, 失败={}", 
                timeoutOrders.size(), successCount, failureCount);

        } catch (Exception e) {
            log.error("超时订单扫描任务执行失败", e);
            // 不抛出异常，避免影响下次定时执行
        }
    }

    /**
     * 处理单个超时订单
     */
    private void processTimeoutOrder(Order order) {
        log.warn("订单超时，强制标记为FAILED: orderId={}, createTime={}, timeoutMinutes={}", 
            order.getOrderId(), order.getCreatedAt(), timeoutMinutes);

        // 1. 更新订单状态为FAILED
        order.markAsFailed("订单处理超时（" + timeoutMinutes + "分钟）");
        int updatedRows = orderMapper.updateStatus(
            order.getOrderId(),
            order.getStatus(),
            null,
            order.getFailureReason()
        );

        if (updatedRows == 0) {
            log.warn("订单状态更新失败，可能已被其他进程处理: orderId={}", order.getOrderId());
            return;
        }

        // 2. 发布订单失败事件，触发补偿机制
        // 由于是超时，无法确定具体在哪个阶段失败，这里使用BALANCE_DEDUCTION作为默认值
        // 补偿监听器会根据实际情况进行补偿（退款、恢复库存等）
        OrderFailedEvent failedEvent = new OrderFailedEvent(
            order.getOrderId(),
            "订单处理超时（" + timeoutMinutes + "分钟）",
            OrderFailedEvent.FailedStage.BALANCE_DEDUCTION
        );
        eventPublisher.publishEvent(failedEvent);

        log.info("超时订单处理完成，已发布失败事件: orderId={}", order.getOrderId());
    }
}
