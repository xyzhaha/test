package com.example.tradesystem.trade.infrastructure.listener;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.common.enums.RechargeStatus;
import com.example.tradesystem.common.model.ExceptionRecord;
import com.example.tradesystem.common.infrastructure.mapper.ExceptionRecordMapper;
import com.example.tradesystem.merchant.application.service.MerchantService;
import com.example.tradesystem.merchant.domain.event.StockRestoredEvent;
import com.example.tradesystem.trade.domain.event.OrderCompletedEvent;
import com.example.tradesystem.trade.domain.event.OrderFailedEvent;
import com.example.tradesystem.trade.domain.model.OrderItem;
import com.example.tradesystem.trade.infrastructure.mapper.OrderItemMapper;
import com.example.tradesystem.user.application.service.UserService;
import com.example.tradesystem.user.domain.event.UserBalanceRefundedEvent;
import com.example.tradesystem.user.domain.model.RechargeRecord;
import com.example.tradesystem.user.infrastructure.mapper.RechargeRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * 订单状态事件监听器 - 处理订单失败补偿和订单完成
 */
@Component
@Slf4j
public class OrderStatusEventListener {

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    @Autowired
    private com.example.tradesystem.trade.infrastructure.mapper.OrderMapper orderMapper;

    @Autowired
    private ExceptionRecordMapper exceptionRecordMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 监听订单失败事件,执行补偿操作
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public void handleOrderFailed(OrderFailedEvent event) {
        log.warn("收到订单失败事件,开始补偿: orderId={}, stage={}, reason={}", 
            event.getOrderId(), event.getFailedStage(), event.getReason());

        try {
            switch (event.getFailedStage()) {
                case BALANCE_DEDUCTION:
                    // 余额扣减失败,无需补偿
                    log.info("余额扣减失败,无需补偿: orderId={}", event.getOrderId());
                    break;

                case INVENTORY_DEDUCTION:
                    // 库存扣减失败,需要退款
                    log.info("库存扣减失败,执行退款: orderId={}", event.getOrderId());
                    
                    // 查询扣款记录获取用户ID和金额
                    RechargeRecord record = rechargeRecordMapper.selectByOrderId(event.getOrderId());
                    if (record != null) {
                        // 执行退款
                        userService.refund(event.getOrderId(), record.getUserId(), record.getAmount().getAmount());
                        
                        // 创建退款记录（使用recharge_record表，requestId设为REFUND_orderId）
                        String refundRecordId = "REF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
                        RechargeRecord refundRecord = new RechargeRecord(
                            refundRecordId,
                            record.getUserId(),
                            record.getAmount().negate(), // 负数表示退款
                            "REFUND_" + event.getOrderId() // requestId用于幂等
                        );
                        refundRecord.setStatus(RechargeStatus.REFUNDED);
                        rechargeRecordMapper.insert(refundRecord);
                        
                        log.info("退款成功: orderId={}, userId={}, amount={}, refundRecordId={}", 
                            event.getOrderId(), record.getUserId(), record.getAmount(), refundRecordId);
                        
                        // 发布退款事件（不触发库存恢复，因为库存未扣减）
                        eventPublisher.publishEvent(new UserBalanceRefundedEvent(
                            event.getOrderId(),
                            record.getUserId(),
                            record.getAmount()
                        ));
                    } else {
                        log.warn("未找到扣款记录，无法退款: orderId={}", event.getOrderId());
                    }
                    break;

                case MERCHANT_CREDIT:
                    // 商家收款失败,需要恢复库存和退款
                    log.info("商家收款失败,执行补偿: orderId={}", event.getOrderId());
                    
                    // 1. 恢复库存
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(event.getOrderId());
                    if (orderItems != null && !orderItems.isEmpty()) {
                        for (OrderItem item : orderItems) {
                            merchantService.restoreStock(
                                event.getOrderId(), 
                                item.getMerchantId(), 
                                item.getSku(), 
                                item.getQuantity()
                            );
                            log.info("库存已恢复: orderId={}, sku={}, quantity={}", 
                                event.getOrderId(), item.getSku(), item.getQuantity());
                        }
                        
                        // 发布库存恢复事件
                        eventPublisher.publishEvent(new StockRestoredEvent(
                            event.getOrderId(),
                            orderItems.get(0).getSku(),
                            orderItems.stream().mapToInt(OrderItem::getQuantity).sum()
                        ));
                    }
                    
                    // 2. 退款
                    RechargeRecord creditRecord = rechargeRecordMapper.selectByOrderId(event.getOrderId());
                    if (creditRecord != null) {
                        userService.refund(event.getOrderId(), creditRecord.getUserId(), creditRecord.getAmount().getAmount());
                        
                        // 创建退款记录
                        String refundRecordId = "REF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
                        RechargeRecord refundRecord = new RechargeRecord(
                            refundRecordId,
                            creditRecord.getUserId(),
                            creditRecord.getAmount().negate(), // 负数表示退款
                            "REFUND_" + event.getOrderId() // requestId用于幂等
                        );
                        refundRecord.setStatus(RechargeStatus.REFUNDED);
                        rechargeRecordMapper.insert(refundRecord);
                        
                        log.info("退款成功: orderId={}, userId={}, amount={}, refundRecordId={}", 
                            event.getOrderId(), creditRecord.getUserId(), creditRecord.getAmount(), refundRecordId);
                        
                        // 注意：这里不发布 UserBalanceRefundedEvent，避免重复恢复库存
                        // 因为库存恢复已经在上面执行了
                    }
                    break;

                default:
                    log.warn("未知的失败阶段: {}", event.getFailedStage());
            }

            // 更新订单状态为FAILED
            java.time.LocalDateTime failTime = java.time.LocalDateTime.now();
            int updatedRows = orderMapper.updateStatus(
                event.getOrderId(), 
                OrderStatus.FAILED, 
                null,
                event.getReason()
            );
            
            if (updatedRows == 0) {
                log.error("订单状态更新失败: orderId={}", event.getOrderId());
                throw new RuntimeException("订单状态更新失败: " + event.getOrderId());
            }
            
            log.info("订单状态已更新为FAILED: orderId={}, reason={}", 
                event.getOrderId(), event.getReason());
            log.info("订单失败补偿完成: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("订单失败补偿异常: orderId={}", event.getOrderId(), e);
            
            // 记录到异常表,人工介入
            String exceptionId = "EXC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            ExceptionRecord exceptionRecord = new ExceptionRecord(
                exceptionId,
                "ORDER_COMPENSATION_FAILED",
                event.getOrderId(),
                "订单失败补偿异常: " + e.getMessage(),
                getStackTrace(e)
            );
            exceptionRecordMapper.insert(exceptionRecord);
            
            log.error("异常已记录: exceptionId={}, orderId={}", exceptionId, event.getOrderId());
            throw e;
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 监听订单完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("收到订单完成事件: orderId={}, completeTime={}", 
            event.getOrderId(), event.getCompleteTime());

        // 这里可以执行订单完成后的后续操作
        // 例如: 发送通知、积分奖励等
        
        log.info("订单完成处理完成: orderId={}", event.getOrderId());
    }

    /**
     * 监听用户余额退款事件
     * 注意：此监听器仅用于记录日志和触发后续业务逻辑，不执行库存恢复
     * 原因：
     * 1. INVENTORY_DEDUCTION失败时，库存未扣减，不应恢复
     * 2. MERCHANT_CREDIT失败时，库存恢复已在handleOrderFailed中执行
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void handleUserBalanceRefunded(UserBalanceRefundedEvent event) {
        log.info("收到用户余额退款事件: orderId={}, userId={}, amount={}", 
            event.getOrderId(), event.getUserId(), event.getRefundedAmount());

        // 这里可以执行退款后的其他业务逻辑，如：
        // - 发送退款通知
        // - 更新用户积分
        // - 记录退款统计等
        
        log.info("用户余额退款处理完成: orderId={}", event.getOrderId());
    }
}
