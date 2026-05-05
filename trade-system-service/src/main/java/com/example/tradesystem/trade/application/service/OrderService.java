package com.example.tradesystem.trade.application.service;

import com.example.tradesystem.common.exception.BusinessException;
import com.example.tradesystem.merchant.domain.model.ProductInventory;
import com.example.tradesystem.merchant.infrastructure.mapper.ProductInventoryMapper;
import com.example.tradesystem.trade.domain.event.OrderCreatedEvent;
import com.example.tradesystem.trade.domain.model.Order;
import com.example.tradesystem.trade.domain.model.OrderItem;
import com.example.tradesystem.trade.infrastructure.mapper.OrderItemMapper;
import com.example.tradesystem.trade.infrastructure.mapper.OrderMapper;
import com.example.tradesystem.user.domain.model.Money;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务 - 核心业务逻辑
 */
@Service
@Slf4j
public class OrderService {

    @Autowired
    private ProductInventoryMapper productInventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private com.example.tradesystem.user.infrastructure.mapper.UserAccountMapper userAccountMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 创建订单(四阶段处理的第一阶段)
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(CreateOrderRequest request) {
        log.info("开始创建订单: userId={}, merchantId={}", request.getUserId(), request.getMerchantId());

        try {
            // 1. 验证商品和计算总金额
            Money totalAmount = validateAndCalculateTotal(request.getItems(), request.getMerchantId());

            // 2. 验证用户余额
            validateUserBalance(request.getUserId(), totalAmount);

            // 3. 生成订单ID
            String orderId = generateOrderId();

            // 4. 创建订单聚合根
            Order order = new Order(orderId, request.getUserId(), request.getMerchantId(), totalAmount);

            // 5. 添加订单项
            List<OrderItem> orderItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = new OrderItem(
                    generateOrderItemId(),
                    orderId,
                    request.getMerchantId(),  // 添加商家ID
                    itemReq.getSku(),
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    new Money(itemReq.getUnitPrice())
                );
                order.addItem(item);
                orderItems.add(item);
            }

            // 6. 保存订单到数据库
            orderMapper.insert(order);
            orderItemMapper.batchInsert(orderItems);

            log.info("订单创建成功: orderId={}, totalAmount={}", orderId, totalAmount);

            // 7. 发布订单创建事件(触发第二阶段:余额扣减)
            log.info("准备发布OrderCreatedEvent: orderId={}", orderId);
            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                orderId,
                request.getUserId(),
                request.getMerchantId(),
                orderItems,
                totalAmount
            );
            eventPublisher.publishEvent(orderCreatedEvent);
            log.info("OrderCreatedEvent已发布: orderId={}, 等待事务提交后触发监听器", orderId);

            return orderId;

        } catch (BusinessException e) {
            log.error("订单创建失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("订单创建异常", e);
            throw new BusinessException("订单创建失败: " + e.getMessage());
        }
    }

    /**
     * 验证商品并计算总金额
     */
    private Money validateAndCalculateTotal(List<CreateOrderRequest.OrderItemRequest> items,Long merchantId) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(400, "订单项不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest item : items) {
            // 验证参数
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(400, "商品数量必须大于0");
            }

            // 查询库存并验证（必须先查询商品信息）
            ProductInventory inventory = productInventoryMapper.selectBySku(
                    merchantId,
                item.getSku()
            );

            if (inventory == null) {
                throw new BusinessException(400, "商品不存在: " + item.getSku());
            }
            //xyz
            // 验证库存是否充足
            if (!inventory.isStockSufficient(item.getQuantity())) {
                throw new BusinessException(400,
                    "库存不足: " + item.getSku() + ", 当前库存: " + inventory.getStockQuantity());
            }

            // 验证商品价格（使用数据库中的价格，防止前端篡改）
            if (inventory.getPrice() == null || !inventory.getPrice().isPositive()) {
                throw new BusinessException(400, "商品价格异常: " + item.getSku());
            }

            // 设置商品名称（从数据库中获取）
            item.setProductName(inventory.getProductName());
            item.setUnitPrice(inventory.getPrice().getAmount());
            // 累加金额（使用数据库中的价格）
            BigDecimal itemTotal = inventory.getPrice().getAmount().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        return new Money(totalAmount);
    }

    /**
     * 验证用户余额是否充足
     */
    private void validateUserBalance(Long userId, Money totalAmount) {
        com.example.tradesystem.user.domain.model.UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException(400, "用户不存在: userId=" + userId);
        }
        
        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new BusinessException(400, 
                String.format("余额不足: 当前余额=%.2f, 需要金额=%.2f", 
                    account.getBalance().getAmount(), totalAmount.getAmount()));
        }
        
        log.info("余额验证通过: userId={}, balance={}, totalAmount={}", 
            userId, account.getBalance(), totalAmount);
    }

    private String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private String generateOrderItemId() {
        return "ITEM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 创建订单请求DTO
     */
    @Data
    public static class CreateOrderRequest {
        
        private Long userId;
        private Long merchantId;
        private List<OrderItemRequest> items;

        @Data
        public static class OrderItemRequest {
            private String sku;
            private String productName;
            private Integer quantity;
            private BigDecimal unitPrice;
            private Long merchantId;
        }
    }
}
