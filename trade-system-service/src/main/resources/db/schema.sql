-- 商品交易系统数据库建表脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS trade_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE trade_system;

-- ============================================
-- 1. 用户模块表
-- ============================================

-- 1.1 用户账户表
CREATE TABLE user_account (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（单位：元）',
    currency VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '货币类型（ISO 4217标准：CNY=人民币）',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- 1.2 充值记录表
CREATE TABLE recharge_record (
    recharge_id VARCHAR(32) PRIMARY KEY COMMENT '充值ID（UUID去掉横线，大写）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '充值金额（单位：元）',
    request_id VARCHAR(64) NOT NULL UNIQUE COMMENT '请求ID（幂等性保证，客户端生成）',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '充值状态（SUCCESS=成功, FAILED=失败）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录表';

-- ============================================
-- 2. 商家模块表
-- ============================================

-- 2.1 商品库存表
CREATE TABLE product_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID（自增）',
    sku VARCHAR(64) NOT NULL COMMENT 'SKU编码（Stock Keeping Unit）',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量（可售数量）',
    price DECIMAL(10,2) NOT NULL COMMENT '商品单价（单位：元）',
    sold_quantity INT NOT NULL DEFAULT 0 COMMENT '已售数量',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_merchant_sku (merchant_id, sku),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品库存表';

-- 2.2 商家账户表
CREATE TABLE merchant_account (
    merchant_id BIGINT PRIMARY KEY COMMENT '商家ID',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额（单位：元）',
    total_income DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入（单位：元）',
    currency VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '货币类型（ISO 4217标准：CNY=人民币）',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家账户表';

-- 2.3 商家收款记录表
CREATE TABLE merchant_credit_record (
    credit_id VARCHAR(32) PRIMARY KEY COMMENT '收款ID（UUID去掉横线，大写）',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '收款金额（单位：元）',
    order_id VARCHAR(32) NOT NULL COMMENT '订单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_id (order_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家收款记录表';

-- 2.4 对账记录表
CREATE TABLE settlement_record (
    settlement_id VARCHAR(32) PRIMARY KEY COMMENT '对账ID（UUID去掉横线，大写）',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    settlement_date DATE NOT NULL COMMENT '对账日期（自然日）',
    total_sales DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '销售总额（单位：元）',
    total_received DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '收款总额（单位：元）',
    difference DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '差异金额（销售总额 - 收款总额）',
    status VARCHAR(20) NOT NULL DEFAULT 'MATCHED' COMMENT '对账状态（MATCHED=一致, MISMATCHED=不一致）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_merchant_date (merchant_id, settlement_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账记录表';

-- ============================================
-- 3. 交易模块表
-- ============================================

-- 3.1 订单表
CREATE TABLE orders (
    order_id VARCHAR(32) PRIMARY KEY COMMENT '订单ID（UUID去掉横线，大写）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态（CREATED=已创建, COMPLETED=已完成, FAILED=失败）',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额（单位：元）',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因（status=FAILED时填写）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 3.2 订单项表
CREATE TABLE order_item (
    order_item_id VARCHAR(32) PRIMARY KEY COMMENT '订单项ID（UUID去掉横线，大写）',
    order_id VARCHAR(32) NOT NULL COMMENT '订单ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    sku VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '购买数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价（单位：元）',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计（quantity × unit_price）',
    INDEX idx_order_id (order_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- ============================================
-- 4. 辅助模块表
-- ============================================

-- 4.1 领域事件记录表
CREATE TABLE domain_event_record (
    event_id VARCHAR(32) PRIMARY KEY COMMENT '事件ID（UUID去掉横线，大写）',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型（如OrderCreatedEvent）',
    payload TEXT NOT NULL COMMENT '事件内容（JSON格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态（PENDING=待处理, PROCESSED=已处理）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领域事件记录表';

-- 4.2 异常记录表
CREATE TABLE exception_record (
    exception_id VARCHAR(32) PRIMARY KEY COMMENT '异常ID（UUID去掉横线，大写）',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型（如ORDER_CREATE、BALANCE_DEDUCTION）',
    business_id VARCHAR(32) NOT NULL COMMENT '业务ID（如订单ID、充值ID）',
    error_message TEXT NOT NULL COMMENT '错误信息',
    stack_trace TEXT COMMENT '堆栈信息（系统异常时填写）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态（PENDING=待处理, RESOLVED=已解决）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_business_type (business_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常记录表';

-- ============================================
-- 初始化测试数据（可选）
-- ============================================

-- 插入测试用户
INSERT INTO user_account (user_id, balance, currency, version) VALUES (1001, 10000.00, 'CNY', 0);
INSERT INTO user_account (user_id, balance, currency, version) VALUES (1002, 5000.00, 'CNY', 0);

-- 插入测试商家
INSERT INTO merchant_account (merchant_id, balance, total_income, currency, version) VALUES (2001, 0.00, 0.00, 'CNY', 0);
INSERT INTO merchant_account (merchant_id, balance, total_income, currency, version) VALUES (2002, 0.00, 0.00, 'CNY', 0);

-- 插入测试商品库存（包含 product_name 字段）
INSERT INTO product_inventory (sku, merchant_id, product_name, stock_quantity, price, sold_quantity, version) 
VALUES ('IPHONE_15_PRO_256G', 2001, 'iPhone 15 Pro 256GB', 100, 8999.00, 0, 0);
INSERT INTO product_inventory (sku, merchant_id, product_name, stock_quantity, price, sold_quantity, version) 
VALUES ('MACBOOK_PRO_14', 2001, 'MacBook Pro 14英寸', 50, 14999.00, 0, 0);
INSERT INTO product_inventory (sku, merchant_id, product_name, stock_quantity, price, sold_quantity, version) 
VALUES ('AIRPODS_PRO', 2002, 'AirPods Pro', 200, 1899.00, 0, 0);
