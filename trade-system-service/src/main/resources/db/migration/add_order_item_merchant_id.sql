-- 为 order_item 表添加 merchant_id 字段
ALTER TABLE order_item 
ADD COLUMN merchant_id BIGINT NOT NULL COMMENT '商家ID' AFTER order_id,
ADD INDEX idx_merchant_id (merchant_id);

-- 更新现有数据（如果有）
-- UPDATE order_item SET merchant_id = ? WHERE order_id = ?;
