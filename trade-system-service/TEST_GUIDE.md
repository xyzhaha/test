# 商品交易系统 - 测试指南

## 📋 测试前准备

### 1. 初始化数据库
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 2. 修改配置
编辑 `src/main/resources/application.yml`,设置正确的数据库连接信息:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/trade_system?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
```

### 3. 启动应用
```bash
cd /Users/xieyuzhu/work/learnWork/demo-base/test/trade-system-service
mvn spring-boot:run
```

等待应用启动成功,看到类似日志:
```
Started TradeSystemServiceApplication in X.XXX seconds
```

---

## 🧪 单元测试

### 运行所有单元测试
```bash
mvn test
```

### 运行指定测试类
```bash
# Money值对象测试
mvn test -Dtest=MoneyTest

# UserAccount聚合根测试
mvn test -Dtest=UserAccountTest

# ProductInventory聚合根测试
mvn test -Dtest=ProductInventoryTest
```

### 预期结果
- ✅ MoneyTest: 9个测试用例全部通过
- ✅ UserAccountTest: 9个测试用例全部通过  
- ✅ ProductInventoryTest: 8个测试用例全部通过

---

## 🔌 API集成测试

### 方式1: 使用自动化脚本(macOS/Linux)

```bash
chmod +x test-api.sh
./test-api.sh
```

脚本会自动执行以下测试流程:
1. 用户充值 1000元
2. 查询用户余额
3. 商家添加库存(100件 iPhone 15 Pro)
4. 查询商品信息
5. 查询商家账户
6. **创建订单**(触发四阶段事件处理)
7. 验证用户余额减少
8. 验证商家余额增加
9. 验证库存减少

### 方式2: 手动执行curl命令

#### 1. 用户充值
```bash
curl -X POST http://localhost:8081/api/v1/users/1001/accounts/recharge \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "requestId": "REQ_001"
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "balance": 1000.00,
    "rechargeId": "RCH_XXXXX"
  }
}
```

#### 2. 查询用户余额
```bash
curl http://localhost:8081/api/v1/users/1001/accounts
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "balance": 1000.00,
    "currency": "CNY"
  }
}
```

#### 3. 商家添加库存
```bash
curl -X POST http://localhost:8081/api/v1/merchants/2001/products/SKU_PHONE_001/stock \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 100,
    "price": 2999.00,
    "productName": "iPhone 15 Pro"
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "sku": "SKU_PHONE_001",
    "merchantId": 2001,
    "productName": "iPhone 15 Pro",
    "price": 2999.00,
    "stockQuantity": 100,
    "soldQuantity": 0
  }
}
```

#### 4. 创建订单(核心测试 - 触发四阶段处理)
```bash
curl -X POST http://localhost:8081/api/v1/trades/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "merchantId": 2001,
    "items": [{
      "sku": "SKU_PHONE_001",
      "productName": "iPhone 15 Pro",
      "quantity": 1,
      "unitPrice": 2999.00,
      "merchantId": 2001
    }]
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": "ORD_XXXXXXXXXXXXXXXXXXXX"
}
```

**关键**: 这个请求会触发完整的事件驱动流程:
1. OrderCreatedEvent → 2. UserBalanceDeductedEvent → 3. InventoryDeductedEvent → 4. OrderCompletedEvent

等待2秒后验证结果。

#### 5. 验证用户余额(应该减少2999元)
```bash
curl http://localhost:8081/api/v1/users/1001/accounts
```

**预期**: balance = 1000.00 - 2999.00 = **-1999.00** (如果初始余额不足会失败)

⚠️ **注意**: 如果余额不足,订单会失败并触发补偿机制。建议先充值足够金额:
```bash
# 先充值5000元
curl -X POST http://localhost:8081/api/v1/users/1001/accounts/recharge \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000.00, "requestId": "REQ_002"}'
```

#### 6. 验证商家余额(应该增加2999元)
```bash
curl http://localhost:8081/api/v1/merchants/2001/accounts
```

**预期**: balance = 0 + 2999.00 = **2999.00**

#### 7. 验证库存(应该减少1件)
```bash
curl http://localhost:8081/api/v1/merchants/2001/products/SKU_PHONE_001
```

**预期**: stockQuantity = 100 - 1 = **99**, soldQuantity = **1**

---

## 🎯 核心测试场景

### 场景1: 正常订单流程
1. 用户充值 5000元
2. 商家添加库存 100件
3. 创建订单购买1件
4. 验证: 用户余额减少、商家余额增加、库存减少

### 场景2: 余额不足
1. 用户余额 100元
2. 创建订单购买2999元商品
3. **预期**: 订单失败,返回"余额不足"错误

### 场景3: 库存不足
1. 库存 5件
2. 创建订单购买10件
3. **预期**: 订单失败,返回"库存不足"错误

### 场景4: 幂等性测试
1. 使用相同的requestId重复充值
2. **预期**: 第二次请求返回相同结果,不会重复充值

### 场景5: 限流测试
在Controller方法上添加`@RateLimit`注解:
```java
@RateLimit(permitsPerSecond = 5.0, limitType = RateLimit.LimitType.USER)
@PostMapping("/{userId}/accounts/recharge")
public ApiResponse<RechargeResponse> recharge(...) {
    // ...
}
```

快速发送超过5次/秒的请求,**预期**: 返回429状态码

---

## 📊 监控日志

启动应用后,观察日志输出,应该看到完整的事件处理流程:

```
INFO  - 开始创建订单: userId=1001, merchantId=2001
INFO  - 订单创建成功: orderId=ORD_XXXXX, totalAmount=2999.00
INFO  - 收到订单创建事件,开始扣减余额: orderId=ORD_XXXXX
INFO  - 开始扣减余额: orderId=ORD_XXXXX, userId=1001, amount=2999.00
INFO  - 余额扣减成功: userId=1001, orderId=ORD_XXXXX
INFO  - 余额扣减成功: orderId=ORD_XXXXX, deductionRecordId=DED_XXXXX
INFO  - 收到余额扣减事件,开始扣减库存: orderId=ORD_XXXXX
INFO  - 开始扣减库存: orderId=ORD_XXXXX, merchantId=2001, sku=SKU_PHONE_001
INFO  - 库存扣减成功: sku=SKU_PHONE_001, orderId=ORD_XXXXX
INFO  - 开始增加商家余额: orderId=ORD_XXXXX, merchantId=2001, amount=2999.00
INFO  - 商家余额增加成功: merchantId=2001, orderId=ORD_XXXXX
INFO  - 库存扣减和商家收款成功: orderId=ORD_XXXXX, creditRecordId=CRD_XXXXX
INFO  - 收到库存扣减事件,标记订单为完成: orderId=ORD_XXXXX
INFO  - 订单已完成: orderId=ORD_XXXXX
INFO  - 收到订单完成事件: orderId=ORD_XXXXX
```

---

## ❗ 常见问题

### Q1: 应用启动失败
**A**: 检查数据库是否正常运行,配置文件中的数据库连接信息是否正确

### Q2: 订单创建失败
**A**: 
- 检查用户是否有足够余额
- 检查商品是否有足够库存
- 查看日志中的具体错误信息

### Q3: 事件没有触发
**A**: 
- 确认使用了`@EventListener`注解
- 确认事件发布使用了`ApplicationEventPublisher`
- 检查事务配置是否正确

### Q4: 单元测试失败
**A**: 
- 确认Maven依赖正确
- 运行`mvn clean test`清理后重新测试
- 查看具体的断言失败信息

---

## 🎉 测试成功标志

✅ 所有单元测试通过  
✅ API接口返回正确响应  
✅ 事件驱动的四阶段处理完整执行  
✅ 用户余额、商家余额、库存数据一致  
✅ 日志中无ERROR级别错误  

恭喜!系统运行正常! 🚀
