-- 为 product_inventory 表添加 product_name 字段
-- 执行此脚本以修复 "Unknown column 'product_name' in 'field list'" 错误

USE trade_system;

-- 添加 product_name 字段
ALTER TABLE product_inventory 
ADD COLUMN product_name VARCHAR(128) NOT NULL COMMENT '商品名称' AFTER merchant_id;

-- 更新现有测试数据的 product_name 字段
UPDATE product_inventory SET product_name = 'iPhone 15 Pro 256GB' WHERE sku = 'IPHONE_15_PRO_256G';
UPDATE product_inventory SET product_name = 'MacBook Pro 14英寸' WHERE sku = 'MACBOOK_PRO_14';
UPDATE product_inventory SET product_name = 'AirPods Pro' WHERE sku = 'AIRPODS_PRO';
