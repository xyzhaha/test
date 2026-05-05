# Trade System Service - 商品交易系统

## 项目概述

基于DDD领域驱动设计和事件驱动架构的商品交易系统,展示高并发处理能力和模块化设计。

## 技术栈

- **Java**: 1.8
- **Spring Boot**: 2.7.18
- **MyBatis**: 2.3.1
- **MySQL**: 8.0
- **Guava**: 31.1 (RateLimiter限流)
- **Lombok**: 简化代码
- **JUnit 5 + Mockito**: 单元测试

## 项目结构

```
com.example.tradesystem
├── common/                     # 通用模块
│   ├── annotation/            # 自定义注解(@RateLimit)
│   ├── config/                # 配置类(WebMvcConfig, RateLimitProperties)
│   ├── event/                 # 领域事件基类
│   ├── exception/             # 异常类和全局异常处理
│   ├── interceptor/           # 限流拦截器
│   └── response/              # 统一响应格式
│
├── user/                      # 用户上下文
│   ├── domain/
│   │   ├── model/            # 聚合根(UserAccount, RechargeRecord)
│   │   └── event/            # 领域事件(UserBalanceDeductedEvent等)
│   ├── application/
│   │   └── service/          # 应用服务(UserService)
│   ├── interface/
│   │   └── controller/       # REST API(UserController)
│   └── infrastructure/
│       ├── mapper/           # MyBatis Mapper
│       └── repository/       # 仓储实现
│
├── merchant/                  # 商家上下文(类似user结构)
│   ├── domain/
│   ├── application/
│   ├── interface/
│   └── infrastructure/
│
└── trade/                     # 交易上下文(类似user结构)
    ├── domain/
    ├── application/
    ├── interface/
    └── infrastructure/
```

## 核心设计

### 1. DDD领域驱动设计

**三个限界上下文**:
- **User Context**: 用户账户管理、充值
- **Merchant Context**: 商家库存管理、对账
- **Trade Context**: 订单交易管理

**聚合根**:
- UserAccount: 用户账户
- ProductInventory: 商品库存
- MerchantAccount: 商家账户
- Order: 订单

### 2. 事件驱动架构

**下单流程(4个阶段)**:
```
阶段1: 创建订单(CREATED) → 发布OrderCreatedEvent
阶段2: 扣减用户余额 → 发布UserBalanceDeductedEvent
阶段3: 扣减库存+增加商家余额 → 发布InventoryDeductedEvent
阶段4: 更新订单状态为COMPLETED
```

**优势**:
- ✅ 缩短事务持有锁时间(< 100ms)
- ✅ 领域间解耦
- ✅ 异步处理,快速响应(< 50ms返回订单ID)
- ✅ 完善补偿机制

### 3. 分层限流策略

**三层架构**:
1. 全局限流: 1000 TPS(可选)
2. 接口级限流: 订单600 TPS、充值200 TPS
3. 用户级限流: 单用户下单10 TPS、充值5 TPS

**实现方式**:
- 自定义注解 `@RateLimit`
- HandlerInterceptor拦截器
- Guava RateLimiter令牌桶算法
- SpEL表达式动态构建限流key

**零代码侵入**:
```java
@PostMapping
@RateLimit(key = "api:order:create", permitsPerSecond = 600)
public ApiResponse<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
    // 业务逻辑,无需任何限流代码
}
```

### 4. 并发控制

**乐观锁**:
```sql
UPDATE user_account 
SET balance = balance - #{amount}, version = version + 1 
WHERE user_id = #{userId} AND version = #{version}
```

**重试机制**:
- 最大重试3次
- 指数退避(100ms, 200ms, 400ms)

**幂等性**:
- 充值: requestId唯一索引
- 事件: eventId去重

## 数据库设计

### 核心表(10张)

**用户模块**:
- user_account: 用户账户
- recharge_record: 充值记录

**商家模块**:
- product_inventory: 商品库存
- merchant_account: 商家账户
- merchant_credit_record: 收款记录
- settlement_record: 对账记录

**交易模块**:
- orders: 订单表
- order_item: 订单项

**辅助模块**:
- domain_event_record: 领域事件记录
- exception_record: 异常记录

详见: `文档/设计/数据库表设计文档.md`

## API接口

### 用户模块
- POST `/api/v1/users/{userId}/accounts/recharge` - 账户充值
- GET `/api/v1/users/{userId}/accounts` - 查询余额
- GET `/api/v1/users/{userId}/accounts/recharges` - 查询充值记录

### 商家模块
- POST `/api/v1/merchants/{merchantId}/products/{sku}/stock` - 添加/更新库存
- GET `/api/v1/merchants/{merchantId}/products/{sku}` - 查询商品信息
- GET `/api/v1/merchants/{merchantId}/accounts` - 查询商家账户
- GET `/api/v1/merchants/{merchantId}/settlements` - 查询对账记录

### 交易模块
- POST `/api/v1/orders` - 创建订单
- GET `/api/v1/orders/{orderId}` - 查询订单详情
- GET `/api/v1/users/{userId}/orders` - 查询用户订单列表

详见: `文档/设计/接口详细设计文档.md`

## 性能指标

| 指标 | 目标值 |
|-----|--------|
| 阶段1响应时间(P95) | < 50ms |
| 完整流程处理时间(P95) | < 2s |
| 并发支持 | 500 TPS |
| 订单创建成功率 | > 99.9% |
| 事务持有锁时间(P95) | < 100ms |

## 快速开始

### 1. 环境准备

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0

### 2. 数据库初始化

执行SQL脚本创建数据库和表结构:
```bash
mysql -u root -p < docs/sql/schema.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/trade_system
    username: your_username
    password: your_password
```

### 4. 编译运行

```bash
mvn clean install
mvn spring-boot:run
```

### 5. 测试接口

```bash
# 用户充值
curl -X POST http://localhost:8081/api/v1/users/1001/accounts/recharge \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "requestId": "REQ_001"}'

# 添加库存
curl -X POST http://localhost:8081/api/v1/merchants/2001/products/IPHONE_15_PRO/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": 100, "price": 8999.00}'

# 创建订单
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "merchantId": 2001,
    "items": [{"sku": "IPHONE_15_PRO", "quantity": 1}]
  }'
```

## 测试

### 单元测试

```bash
mvn test
```

覆盖率目标: ≥ 80%

### 压力测试

使用JMeter进行压测:
- 并发充值: 100 TPS
- 并发下单: 500 TPS

## 文档

- [架构全景设计文档](../../文档/设计/架构全景设计文档2.md)
- [技术概要设计文档](../../文档/设计/技术概要设计文档.md)
- [技术详细设计文档](../../文档/设计/技术详细设计文档.md)
- [接口详细设计文档](../../文档/设计/接口详细设计文档.md)
- [数据库表设计文档](../../文档/设计/数据库表设计文档.md)

## 待办事项

当前项目已完成基础框架搭建,后续需要实现:

### Phase 1: 基础功能
- [ ] 用户模块(UserContext)完整实现
  - [ ] Domain Model(UserAccount, RechargeRecord)
  - [ ] Service层(UserService)
  - [ ] Controller层(UserController)
  - [ ] MyBatis Mapper
  - [ ] 单元测试
  
- [ ] 商家模块(MerchantContext)完整实现
  - [ ] Domain Model(ProductInventory, MerchantAccount)
  - [ ] Service层(MerchantService)
  - [ ] Controller层(MerchantController)
  - [ ] MyBatis Mapper
  - [ ] 单元测试
  
- [ ] 交易模块(TradeContext)完整实现
  - [ ] Domain Model(Order, OrderItem)
  - [ ] Service层(OrderService)
  - [ ] Controller层(TradeController)
  - [ ] MyBatis Mapper
  - [ ] 单元测试

### Phase 2: 事件驱动
- [ ] 领域事件定义
  - [ ] OrderCreatedEvent
  - [ ] UserBalanceDeductedEvent
  - [ ] InventoryDeductedEvent
  - [ ] OrderCompletedEvent
  - [ ] OrderFailedEvent
  
- [ ] 事件监听器
  - [ ] UserBalanceEventListener
  - [ ] InventoryEventListener
  - [ ] OrderStatusEventListener

### Phase 3: 高级特性
- [ ] 限流拦截器完整实现
- [ ] 乐观锁重试机制
- [ ] 补偿机制实现
- [ ] 对账定时任务
- [ ] 全局异常处理

### Phase 4: 测试与优化
- [ ] 集成测试
- [ ] 压力测试
- [ ] 性能优化
- [ ] 监控告警

## 参考资料

- 《领域驱动设计:软件核心复杂性应对之道》- Eric Evans
- 《实现领域驱动设计》- Vaughn Vernon
- 阿里巴巴Java开发手册
- Spring Framework官方文档
- MyBatis官方文档

## License

MIT
