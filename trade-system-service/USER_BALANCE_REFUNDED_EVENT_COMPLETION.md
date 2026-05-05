# UserBalanceRefundedEvent 补全报告

## 检查时间
2026-05-03

## 一、事件定义检查

### ✅ UserBalanceRefundedEvent.java
**位置**: `user/domain/event/UserBalanceRefundedEvent.java`

**状态**: 已完整实现

```java
@Getter
public class UserBalanceRefundedEvent extends DomainEvent {
    private final String orderId;
    private final Long userId;
    private final Money refundedAmount;

    public UserBalanceRefundedEvent(String orderId, Long userId, Money refundedAmount) {
        super(orderId, "USER_BALANCE_REFUNDED");
        this.orderId = orderId;
        this.userId = userId;
        this.refundedAmount = refundedAmount;
    }
}
```

**字段说明**:
- `orderId`: 订单ID
- `userId`: 用户ID
- `refundedAmount`: 退款金额（Money类型）

---

## 二、事件发布点检查

### ✅ INVENTORY_DEDUCTION 场景
**位置**: `OrderStatusEventListener.handleOrderFailed()` - case INVENTORY_DEDUCTION

**状态**: 已补全退款记录功能

**修复内容**:
1. ✅ 执行退款操作
2. ✅ **创建退款记录**（新增）
3. ✅ 发布 UserBalanceRefundedEvent

**代码**:
```java
case INVENTORY_DEDUCTION:
    // 库存扣减失败,需要退款
    RechargeRecord record = rechargeRecordMapper.selectByOrderId(event.getOrderId());
    if (record != null) {
        // 1. 执行退款
        userService.refund(event.getOrderId(), record.getUserId(), record.getAmount().getAmount());
        
        // 2. 创建退款记录（新增）
        String refundRecordId = "REF_" + UUID.randomUUID()...;
        RechargeRecord refundRecord = new RechargeRecord(
            refundRecordId,
            record.getUserId(),
            record.getAmount().negate(), // 负数表示退款
            "REFUND_" + event.getOrderId()
        );
        refundRecord.setStatus(RechargeStatus.REFUNDED);
        rechargeRecordMapper.insert(refundRecord);
        
        // 3. 发布退款事件
        eventPublisher.publishEvent(new UserBalanceRefundedEvent(...));
    }
    break;
```

---

### ✅ MERCHANT_CREDIT 场景
**位置**: `OrderStatusEventListener.handleOrderFailed()` - case MERCHANT_CREDIT

**状态**: 已补全退款记录功能（但不发布事件，避免重复恢复库存）

**修复内容**:
1. ✅ 恢复库存
2. ✅ 执行退款操作
3. ✅ **创建退款记录**（新增）
4. ❌ **不发布** UserBalanceRefundedEvent（设计如此，避免重复恢复库存）

**代码**:
```java
case MERCHANT_CREDIT:
    // 1. 恢复库存
    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(event.getOrderId());
    for (OrderItem item : orderItems) {
        merchantService.restoreStock(...);
    }
    eventPublisher.publishEvent(new StockRestoredEvent(...));
    
    // 2. 退款
    RechargeRecord creditRecord = rechargeRecordMapper.selectByOrderId(event.getOrderId());
    if (creditRecord != null) {
        userService.refund(...);
        
        // 创建退款记录（新增）
        String refundRecordId = "REF_" + UUID.randomUUID()...;
        RechargeRecord refundRecord = new RechargeRecord(...);
        refundRecord.setStatus(RechargeStatus.REFUNDED);
        rechargeRecordMapper.insert(refundRecord);
        
        // 注意：这里不发布 UserBalanceRefundedEvent，避免重复恢复库存
    }
    break;
```

---

## 三、事件监听器检查

### ✅ handleUserBalanceRefunded()
**位置**: `OrderStatusEventListener.handleUserBalanceRefunded()`

**状态**: 已实现（仅记录日志，不执行库存恢复）

**设计说明**:
此监听器**不应该恢复库存**，原因：
1. **INVENTORY_DEDUCTION 失败时**：库存未扣减，不应恢复
2. **MERCHANT_CREDIT 失败时**：库存恢复已在 `handleOrderFailed()` 中执行

**当前实现**:
```java
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

**未来扩展**:
- 发送退款通知（短信/邮件）
- 更新用户积分
- 记录退款统计数据
- 触发风控检查

---

## 四、依赖组件补全

### ✅ Money.negate() 方法
**位置**: `user/domain/model/Money.java`

**状态**: 已添加

**代码**:
```java
/**
 * 取反（用于退款等场景）
 */
public Money negate() {
    return new Money(this.amount.negate(), this.currency, true); // 允许负数
}
```

**设计说明**:
- 使用私有构造函数支持负数金额
- 公开构造函数仍然禁止负数（保证业务规则）
- negate() 方法内部使用允许负数的构造函数

---

### ✅ RechargeStatus.REFUNDED 枚举值
**位置**: `common/enums/RechargeStatus.java`

**状态**: 已添加

**代码**:
```java
public enum RechargeStatus {
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    REFUNDED("REFUNDED", "已退款");  // 新增
}
```

---

## 五、数据库表设计

### recharge_record 表
**用途**: 存储充值和退款记录

**字段说明**:
| 字段 | 类型 | 说明 |
|-----|------|------|
| recharge_id | VARCHAR(32) | 记录ID（主键） |
| user_id | BIGINT | 用户ID |
| amount | DECIMAL(10,2) | 金额（正数=充值，负数=退款） |
| request_id | VARCHAR(64) | 请求ID（唯一索引，幂等性保证） |
| status | VARCHAR(20) | 状态（SUCCESS/FAILED/REFUNDED） |
| created_at | DATETIME | 创建时间 |

**退款记录示例**:
```sql
INSERT INTO recharge_record (recharge_id, user_id, amount, request_id, status) 
VALUES ('REF_20260503_00001', 1001, -8999.00, 'REFUND_ORD_20260503_001', 'REFUNDED');
```

**幂等性保证**:
- requestId 设置为 `REFUND_{orderId}`
- 相同 orderId 的重复退款会被唯一索引拦截

---

## 六、完整的事件流转图

### 正向流程
```
OrderCreatedEvent 
  → UserBalanceDeductedEvent 
  → InventoryDeductedEvent 
  → OrderCompletedEvent
```

### 失败补偿流程

#### 场景1: BALANCE_DEDUCTION（余额扣减失败）
```
OrderFailedEvent(BALANCE_DEDUCTION)
  → handleOrderFailed()
  → 无需补偿（余额未扣减）
  → 更新订单为 FAILED
```

#### 场景2: INVENTORY_DEDUCTION（库存扣减失败）
```
OrderFailedEvent(INVENTORY_DEDUCTION)
  → handleOrderFailed()
  → 退款
  → 创建退款记录（amount为负数）
  → 发布 UserBalanceRefundedEvent
  → handleUserBalanceRefunded()（记录日志）
  → 更新订单为 FAILED
```

#### 场景3: MERCHANT_CREDIT（商家收款失败）
```
OrderFailedEvent(MERCHANT_CREDIT)
  → handleOrderFailed()
  → 恢复库存
  → 发布 StockRestoredEvent
  → 退款
  → 创建退款记录（amount为负数）
  → 不发布 UserBalanceRefundedEvent（避免重复恢复库存）
  → 更新订单为 FAILED
```

---

## 七、修复总结

### 已完成的修复

1. ✅ **Money.negate() 方法** - 支持退款场景的负数金额
2. ✅ **RechargeStatus.REFUNDED** - 新增退款状态枚举
3. ✅ **INVENTORY_DEDUCTION 场景** - 添加退款记录持久化
4. ✅ **MERCHANT_CREDIT 场景** - 添加退款记录持久化
5. ✅ **handleUserBalanceRefunded()** - 明确注释说明不恢复库存的原因

### 设计亮点

1. **幂等性保证**: 退款记录的 requestId 使用 `REFUND_{orderId}` 格式
2. **金额符号约定**: 正数表示充值，负数表示退款
3. **避免重复恢复**: MERCHANT_CREDIT 场景不发布 UserBalanceRefundedEvent
4. **可扩展性**: handleUserBalanceRefunded() 预留了扩展点（通知、积分等）

### 数据一致性保证

1. **退款记录**: 每次退款都会创建一条记录到 recharge_record 表
2. **事务安全**: 所有操作在同一个事务中完成
3. **异常处理**: 失败时记录到 exception_record 表，人工介入

---

## 八、测试建议

### 单元测试
1. 测试 Money.negate() 方法
2. 测试退款记录创建逻辑
3. 测试 UserBalanceRefundedEvent 的发布和监听

### 集成测试
1. 测试 INVENTORY_DEDUCTION 场景的完整补偿流程
2. 测试 MERCHANT_CREDIT 场景的完整补偿流程
3. 验证退款记录的幂等性（重复退款）

### 压力测试
1. 并发退款场景
2. 验证退款记录的唯一索引是否生效

---

## 九、后续优化建议

1. **退款通知**: 在 handleUserBalanceRefunded() 中添加短信/邮件通知
2. **积分回退**: 如果系统有积分功能，退款时回退积分
3. **风控检查**: 频繁退款触发风控告警
4. **对账支持**: T+1 对账时包含退款记录
5. **监控指标**: 添加退款成功率、退款时长等监控指标

---

## 十、相关文件清单

### 事件相关
- `user/domain/event/UserBalanceRefundedEvent.java` ✅
- `trade/infrastructure/listener/OrderStatusEventListener.java` ✅

### 模型相关
- `user/domain/model/Money.java` ✅（新增 negate() 方法）
- `user/domain/model/RechargeRecord.java` ✅
- `common/enums/RechargeStatus.java` ✅（新增 REFUNDED）

### Mapper 相关
- `user/infrastructure/mapper/RechargeRecordMapper.java` ✅
- `resources/mapper/RechargeRecordMapper.xml` ✅

---

**文档状态**: 已完成  
**审核状态**: 待评审  
**下一步**: 编写单元测试和集成测试
