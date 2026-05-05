#!/bin/bash

# 商品交易系统 - API测试脚本
# 使用前请确保应用已启动: mvn spring-boot:run

BASE_URL="http://localhost:8081/api/v1"

echo "========================================="
echo "商品交易系统 - API集成测试"
echo "========================================="
echo ""

# 1. 用户充值
echo "1. 用户充值 (userId=1001, amount=1000)"
RESPONSE=$(curl -s -X POST "$BASE_URL/users/1001/accounts/recharge" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "requestId": "REQ_'$(date +%s)'"
  }')
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 2. 查询用户余额
echo "2. 查询用户余额 (userId=1001)"
RESPONSE=$(curl -s "$BASE_URL/users/1001/accounts")
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 3. 商家添加库存
echo "3. 商家添加库存 (merchantId=2001, SKU=SKU_PHONE_001)"
RESPONSE=$(curl -s -X POST "$BASE_URL/merchants/2001/products/SKU_PHONE_001/stock" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 100,
    "price": 2999.00,
    "productName": "iPhone 15 Pro"
  }')
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 4. 查询商品信息
echo "4. 查询商品信息 (merchantId=2001, SKU=SKU_PHONE_001)"
RESPONSE=$(curl -s "$BASE_URL/merchants/2001/products/SKU_PHONE_001")
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 5. 查询商家账户
echo "5. 查询商家账户 (merchantId=2001)"
RESPONSE=$(curl -s "$BASE_URL/merchants/2001/accounts")
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 6. 创建订单(触发四阶段处理)
echo "6. 创建订单 (触发事件驱动的四阶段处理)"
RESPONSE=$(curl -s -X POST "$BASE_URL/trades/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "merchantId": 2001,
    "items": [{
      "sku": "MACBOOK_PRO_14",
      "quantity": 1,
      "merchantId": 2001
    }]
  }')
echo "$RESPONSE" | python3 -m json.tool
ORDER_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['data'])" 2>/dev/null)
echo "订单ID: $ORDER_ID"
echo ""

# 等待事件处理完成
sleep 2

# 7. 再次查询用户余额(应该减少)
echo "7. 查询用户余额(订单处理后)"
RESPONSE=$(curl -s "$BASE_URL/users/1001/accounts")
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 8. 再次查询商家账户(应该增加)
echo "8. 查询商家账户(订单处理后)"
RESPONSE=$(curl -s "$BASE_URL/merchants/2001/accounts")
echo "$RESPONSE" | python3 -m json.tool
echo ""

# 9. 再次查询库存(应该减少)
echo "9. 查询库存(订单处理后)"
RESPONSE=$(curl -s "$BASE_URL/merchants/2001/products/MACBOOK_PRO_14")
echo "$RESPONSE" | python3 -m json.tool
echo ""

echo "========================================="
echo "测试完成!"
echo "========================================="
