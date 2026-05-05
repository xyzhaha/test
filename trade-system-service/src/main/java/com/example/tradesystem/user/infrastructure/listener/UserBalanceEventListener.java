package com.example.tradesystem.user.infrastructure.listener;

import com.example.tradesystem.trade.domain.event.OrderCreatedEvent;
import com.example.tradesystem.trade.domain.event.OrderFailedEvent;
import com.example.tradesystem.user.application.service.UserService;
import com.example.tradesystem.user.domain.event.UserBalanceDeductedEvent;
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

import java.util.UUID;

/**
 * 用户余额事件监听器 - 处理第二阶段:余额扣减
 */
@Component
@Slf4j
public class UserBalanceEventListener {

    @Autowired
    private UserService userService;

    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 监听订单创建事件,扣减用户余额
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("✅ 事务已提交，收到订单创建事件: orderId={}, userId={}, amount={}", 
            event.getOrderId(), event.getUserId(), event.getTotalAmount());

        try {
            // 1. 扣减余额
            userService.deductBalance(event.getOrderId(), event.getUserId(), event.getTotalAmount().getAmount());

            // 2. 创建扣款记录(使用recharge_record表,requestId设为orderId)
            String deductionRecordId = "DED_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            RechargeRecord deductionRecord = new RechargeRecord(
                deductionRecordId,
                event.getUserId(),
                event.getTotalAmount(),
                event.getOrderId()  // requestId设为orderId用于幂等
            );
            rechargeRecordMapper.insert(deductionRecord);

            log.info("余额扣减成功: orderId={}, deductionRecordId={}", event.getOrderId(), deductionRecordId);

            // 3. 发布余额扣减成功事件(触发第三阶段:库存扣减和商家收款)
            log.info("准备发布UserBalanceDeductedEvent: orderId={}", event.getOrderId());
            UserBalanceDeductedEvent balanceDeductedEvent = new UserBalanceDeductedEvent(
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                deductionRecordId
            );
            eventPublisher.publishEvent(balanceDeductedEvent);
            log.info("✅ UserBalanceDeductedEvent已发布: orderId={}, 将触发库存扣减", event.getOrderId());

        } catch (Exception e) {
            log.error("余额扣减失败: orderId={}", event.getOrderId(), e);
            
            // 发布订单失败事件
            eventPublisher.publishEvent(new OrderFailedEvent(
                event.getOrderId(),
                "余额扣减失败: " + e.getMessage(),
                OrderFailedEvent.FailedStage.BALANCE_DEDUCTION
            ));
            
            throw e;
        }
    }
}
