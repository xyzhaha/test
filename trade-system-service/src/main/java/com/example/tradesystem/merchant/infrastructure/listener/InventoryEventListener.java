package com.example.tradesystem.merchant.infrastructure.listener;

import com.example.tradesystem.common.enums.OrderStatus;
import com.example.tradesystem.merchant.application.service.MerchantService;
import com.example.tradesystem.merchant.domain.event.InventoryDeductedEvent;
import com.example.tradesystem.merchant.domain.model.MerchantCreditRecord;
import com.example.tradesystem.merchant.infrastructure.mapper.MerchantCreditRecordMapper;
import com.example.tradesystem.trade.domain.event.OrderCompletedEvent;
import com.example.tradesystem.trade.domain.event.OrderFailedEvent;
import com.example.tradesystem.trade.domain.model.OrderItem;
import com.example.tradesystem.trade.infrastructure.mapper.OrderItemMapper;
import com.example.tradesystem.trade.infrastructure.mapper.OrderMapper;
import com.example.tradesystem.user.domain.event.UserBalanceDeductedEvent;
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
 * 库存事件监听器 - 处理第三阶段:库存扣减和商家收款
 */
@Component
@Slf4j
public class InventoryEventListener {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private MerchantCreditRecordMapper creditRecordMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 监听余额扣减事件,扣减库存并增加商家余额
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public void handleUserBalanceDeducted(UserBalanceDeductedEvent event) {
        log.info("收到余额扣减事件,开始扣减库存: orderId={}", event.getOrderId());

        try {
            // 1. 查询订单项列表
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(event.getOrderId());
            if (orderItems == null || orderItems.isEmpty()) {
                throw new RuntimeException("订单项不存在: " + event.getOrderId());
            }

            // 2. 遍历订单项，扣减每个商品的库存
            Long merchantId = null;
            for (OrderItem item : orderItems) {
                merchantId = item.getMerchantId(); // 从订单项获取商家ID
                
                // 扣减库存
                merchantService.deductStock(event.getOrderId(), merchantId, item.getSku(), item.getQuantity());
                
                log.info("库存扣减成功: orderId={}, sku={}, quantity={}", 
                    event.getOrderId(), item.getSku(), item.getQuantity());
            }

            // 3. 增加商家余额（使用订单总金额）
            merchantService.creditMerchant(event.getOrderId(), merchantId, event.getDeductedAmount().getAmount());

            // 4. 创建收款记录
            String creditRecordId = "CRD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            MerchantCreditRecord creditRecord = new MerchantCreditRecord(
                creditRecordId,
                merchantId,
                event.getDeductedAmount(),
                event.getOrderId()
            );
            creditRecordMapper.insert(creditRecord);

            log.info("库存扣减和商家收款成功: orderId={}, creditRecordId={}", event.getOrderId(), creditRecordId);

            // 5. 发布库存扣减事件（触发第四阶段）
            eventPublisher.publishEvent(new InventoryDeductedEvent(
                event.getOrderId(),
                merchantId,
                orderItems.get(0).getSku(), // 使用第一个SKU作为代表
                orderItems.stream().mapToInt(OrderItem::getQuantity).sum(), // 总数量
                creditRecordId
            ));

        } catch (Exception e) {
            log.error("库存扣减或商家收款失败: orderId={}", event.getOrderId(), e);
            
            // 判断是库存扣减失败还是商家收款失败
            // 如果是因为库存不足导致的失败，发布 INVENTORY_DEDUCTION
            // 如果是商家收款失败，需要恢复库存并发布 MERCHANT_CREDIT
            // 这里简化处理：统一发布 INVENTORY_DEDUCTION，由补偿机制处理
            // 注意：如果库存已扣减但商家收款失败，需要在补偿时恢复库存
            
            eventPublisher.publishEvent(new OrderFailedEvent(
                event.getOrderId(),
                "库存扣减或商家收款失败: " + e.getMessage(),
                OrderFailedEvent.FailedStage.INVENTORY_DEDUCTION
            ));
            
            throw e;
        }
    }

    /**
     * 监听库存扣减事件,标记订单为完成(第四阶段)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void handleInventoryDeducted(InventoryDeductedEvent event) {
        log.info("收到库存扣减事件,标记订单为完成: orderId={}", event.getOrderId());

        try {
            // 1. 更新订单状态为COMPLETED
            java.time.LocalDateTime completeTime = java.time.LocalDateTime.now();
            int updatedRows = orderMapper.updateStatus(
                event.getOrderId(), 
                OrderStatus.COMPLETED, 
                completeTime, 
                null
            );
            
            if (updatedRows == 0) {
                throw new RuntimeException("订单状态更新失败: " + event.getOrderId());
            }
            
            log.info("订单状态已更新为COMPLETED: orderId={}", event.getOrderId());
            
            // 2. 发布订单完成事件
            eventPublisher.publishEvent(new OrderCompletedEvent(
                event.getOrderId(),
                completeTime
            ));

            log.info("订单已完成: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("订单完成处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
