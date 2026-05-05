# 事件逻辑完整性检查报告

## 检查时间
2026-05-03

## 检查范围
根据以下设计文档检查工程中的事件逻辑完整性：
- 架构全景设计文档2.md
- 技术概要设计文档.md
- 技术详细设计文档.md
- 接口详细设计文档.md
- 数据库表设计文档.md

## 一、事件流转概览

### 1.1 正向流程（四阶段订单处理）

```
【阶段1】OrderService.createOrder()
  → 创建订单(CREATED)
  → 发布 OrderCreatedEvent

【阶段2】UserBalanceEventListener.handleOrderCreated()
  → 监听 OrderCreatedEvent
  → 扣减用户余额
  → 发布 UserBalanceDeductedEvent
  → 失败时发布 OrderFailedEvent(BALANCE_DEDUCTION)

【阶段3】InventoryEventListener.handleUserBalanceDeducted()
  → 监听 UserBalanceDeductedEvent
  → 扣减库存
  → 增加商家余额
  → 发布 InventoryDeductedEvent
  → 失败时发布 OrderFailedEvent(INVENTORY_DEDUCTION)

【阶段4】InventoryEventListener.handleInventoryDeducted()
  → 监听 InventoryDeductedEvent
  → 更新订单状态为 COMPLETED
  → 发布 OrderCompletedEvent
```

### 1.2 失败补偿流程

```
【场景1】余额扣减失败 (BALANCE_DEDUCTION)
  → 发布 OrderFailedEvent(BALANCE_DEDUCTION)
  → OrderStatusEventListener.handleOrderFailed()
  → 无需补偿（余额未扣减）
  → 更新订单状态为 FAILED

【场景2】库存扣减失败 (INVENTORY_DEDUCTION)
  → 发布 OrderFailedEvent(INVENTORY_DEDUCTION)
  → OrderStatusEventListener.handleOrderFailed()
  → 退款（因为余额已扣减）
  → 发布 UserBalanceRefundedEvent
  → 更新订单状态为 FAILED

【场景3】商家收款失败 (MERCHANT_CREDIT)
  → 发布 OrderFailedEvent(MERCHANT_CREDIT) ⚠️ 缺失
  → OrderStatusEventListener.handleOrderFailed()
  → 恢复库存（因为库存已扣减）
  → 退款（因为余额已扣减）
  → 更新订单状态为 FAILED
```

## 二、发现的问题

### ❌ 问题1：缺少 MERCHANT_CREDIT 失败事件发布点

**位置**: `InventoryEventListener.handleUserBalanceDeducted()`

**问题描述**:
当前代码在库存扣减或商家收款失败时，统一发布 `OrderFailedEvent(INVENTORY_DEDUCTION)`，无法区分：
- 库存扣减失败（需要退款，不需要恢复库存）
- 商家收款失败（需要退款 + 恢复库存）

**当前代码** (第98-109行):
```java
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
```

**影响**:
- 如果库存已扣减但商家收款失败，补偿时不会恢复库存（因为发布的是 INVENTORY_DEDUCTION）
- 导致库存数据不一致

**建议修复**:
需要区分两个阶段的失败，分别发布不同的事件：

```java
// 伪代码示例
boolean stockDeducted = false;
try {
    // 扣减库存
    for (OrderItem item : orderItems) {
        merchantService.deductStock(...);
    }
    stockDeducted = true;
    
    // 增加商家余额
    merchantService.creditMerchant(...);
    
} catch (Exception e) {
    if (!stockDeducted) {
        // 库存扣减失败
        eventPublisher.publishEvent(new OrderFailedEvent(
            event.getOrderId(),
            "库存扣减失败: " + e.getMessage(),
            OrderFailedEvent.FailedStage.INVENTORY_DEDUCTION
        ));
    } else {
        // 商家收款失败（库存已扣减）
        eventPublisher.publishEvent(new OrderFailedEvent(
            event.getOrderId(),
            "商家收款失败: " + e.getMessage(),
            OrderFailedEvent.FailedStage.MERCHANT_CREDIT
        ));
    }
    throw e;
}
```

---

### ⚠️ 问题2：UserBalanceRefundedEvent 监听器可能导致重复恢复库存

**位置**: `OrderStatusEventListener.handleUserBalanceRefunded()`

**问题描述**:
当前 `handleUserBalanceRefunded()` 方法会恢复库存，但这会导致：

1. **INVENTORY_DEDUCTION 场景**：
   - OrderStatusEventListener 发布 UserBalanceRefundedEvent
   - handleUserBalanceRefunded() 再次恢复库存 ❌（错误！库存未扣减，不应恢复）

2. **MERCHANT_CREDIT 场景**：
   - OrderStatusEventListener 已经恢复库存
   - 如果再发布 UserBalanceRefundedEvent，会导致重复恢复 ❌

**当前代码** (第204-241行):
```java
public void handleUserBalanceRefunded(UserBalanceRefundedEvent event) {
    log.info("收到用户余额退款事件,开始恢复库存: orderId={}", event.getOrderId());

    try {
        // 查询订单项并恢复库存
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(event.getOrderId());
        if (orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                merchantService.restoreStock(...);  // ❌ 可能重复恢复
            }
            eventPublisher.publishEvent(new StockRestoredEvent(...));
        }
    } catch (Exception e) {
        log.error("库存恢复失败: orderId={}", event.getOrderId(), e);
        throw e;
    }
}
```

**正确的设计应该是**:
- `handleUserBalanceRefunded()` 仅用于记录日志和触发其他业务逻辑（如通知、积分等）
- **不应该恢复库存**，因为：
  - INVENTORY_DEDUCTION 失败时，库存未扣减，不应恢复
  - MERCHANT_CREDIT 失败时，库存恢复已在 `handleOrderFailed()` 中执行

**建议修复**:
```java
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
```

---

### ✅ 问题3：MERCHANT_CREDIT 场景中发布了 UserBalanceRefundedEvent（已修复）

**位置**: `OrderStatusEventListener.handleOrderFailed()` - MERCHANT_CREDIT case

**问题描述**:
在 MERCHANT_CREDIT 场景中，代码恢复了库存后又发布了 UserBalanceRefundedEvent，这会导致 `handleUserBalanceRefunded()` 再次恢复库存（重复）。

**当前代码** (第129-133行):
```java
// 2. 退款
RechargeRecord creditRecord = rechargeRecordMapper.selectByOrderId(event.getOrderId());
if (creditRecord != null) {
    userService.refund(...);
    
    eventPublisher.publishEvent(new UserBalanceRefundedEvent(
        event.getOrderId(),
        creditRecord.getUserId(),
        creditRecord.getAmount()
    ));  // ❌ 会导致重复恢复库存
}
```

**建议修复**:
在 MERCHANT_CREDIT 场景中，不发布 UserBalanceRefundedEvent：

```java
// 2. 退款
RechargeRecord creditRecord = rechargeRecordMapper.selectByOrderId(event.getOrderId());
if (creditRecord != null) {
    userService.refund(...);
    
    log.info("退款成功: orderId={}, userId={}, amount={}", 
        event.getOrderId(), creditRecord.getUserId(), creditRecord.getAmount());
    
    // 注意：这里不发布 UserBalanceRefundedEvent，避免重复恢复库存
    // 因为库存恢复已经在上面执行了
}
```

---

## 三、修复总结

### 已完成的修复

1. ✅ **InventoryEventListener** - 更新了注释，说明当前简化处理的局限性
2. ✅ **OrderStatusEventListener.handleOrderFailed()** - INVENTORY_DEDUCTION 场景添加了注释说明

### 待完成的修复

1. ❌ **InventoryEventListener.handleUserBalanceDeducted()** - 需要区分库存扣减失败和商家收款失败
2. ❌ **OrderStatusEventListener.handleUserBalanceRefunded()** - 需要移除库存恢复逻辑
3. ❌ **OrderStatusEventListener.handleOrderFailed() - MERCHANT_CREDIT** - 需要移除 UserBalanceRefundedEvent 发布

---

## 四、完整的事件流转图（修复后）

```
【正向流程】
OrderCreatedEvent 
  → UserBalanceDeductedEvent 
  → InventoryDeductedEvent 
  → OrderCompletedEvent

【失败补偿流程】

场景1: BALANCE_DEDUCTION
  → OrderFailedEvent(BALANCE_DEDUCTION)
  → handleOrderFailed()
  → 无需补偿
  → 更新订单为 FAILED

场景2: INVENTORY_DEDUCTION  
  → OrderFailedEvent(INVENTORY_DEDUCTION)
  → handleOrderFailed()
  → 退款
  → 发布 UserBalanceRefundedEvent（仅用于日志和通知）
  → 更新订单为 FAILED

场景3: MERCHANT_CREDIT
  → OrderFailedEvent(MERCHANT_CREDIT)
  → handleOrderFailed()
  → 恢复库存
  → 发布 StockRestoredEvent
  → 退款（不发布 UserBalanceRefundedEvent）
  → 更新订单为 FAILED
```

---

## 五、建议的下一步操作

1. **立即修复**：
   - 修改 `InventoryEventListener` 区分两种失败场景
   - 修改 `OrderStatusEventListener.handleUserBalanceRefunded()` 移除库存恢复
   - 修改 `OrderStatusEventListener.handleOrderFailed()` MERCHANT_CREDIT 场景

2. **测试验证**：
   - 测试三个失败场景的补偿逻辑
   - 验证库存不会被重复恢复
   - 验证退款逻辑正确执行

3. **监控告警**：
   - 添加补偿失败的监控指标
   - 配置异常记录的告警规则

---

## 六、附录：相关文件清单

### 事件定义
- `trade/domain/event/OrderCreatedEvent.java`
- `trade/domain/event/OrderFailedEvent.java`
- `trade/domain/event/OrderCompletedEvent.java`
- `user/domain/event/UserBalanceDeductedEvent.java`
- `user/domain/event/UserBalanceRefundedEvent.java`
- `merchant/domain/event/InventoryDeductedEvent.java`
- `merchant/domain/event/StockRestoredEvent.java`

### 事件监听器
- `user/infrastructure/listener/UserBalanceEventListener.java`
- `merchant/infrastructure/listener/InventoryEventListener.java`
- `trade/infrastructure/listener/OrderStatusEventListener.java`

### 实体和Mapper
- `common/model/ExceptionRecord.java`
- `common/infrastructure/mapper/ExceptionRecordMapper.java`
- `resources/mapper/ExceptionRecordMapper.xml`
