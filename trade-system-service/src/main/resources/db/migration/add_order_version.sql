-- 为 orders 表添加 version 和 updated_at 字段
-- 为 order_item 表添加 product_name 和 merchant_id 字段
-- 执行此脚本以修复数据库字段缺失错误

USE trade_system;

-- 1. 为 orders 表添加 version 字段
ALTER TABLE orders 
ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）' AFTER total_amount;

-- 2. 为 orders 表添加 updated_at 字段
ALTER TABLE orders 
ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER failure_reason;

-- 3. 为 order_item 表添加 merchant_id 字段
ALTER TABLE order_item 
ADD COLUMN merchant_id BIGINT NOT NULL COMMENT '商家ID' AFTER order_id;

-- 4. 为 order_item 表添加 product_name 字段
ALTER TABLE order_item 
ADD COLUMN product_name VARCHAR(128) NOT NULL COMMENT '商品名称' AFTER sku;

-- 5. 为 order_item 表添加 merchant_id 索引
ALTER TABLE order_item 
ADD INDEX idx_merchant_id (merchant_id);
