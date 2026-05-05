package com.example.tradesystem.merchant.application.service;

import com.example.tradesystem.common.exception.BusinessException;
import com.example.tradesystem.common.exception.ResourceNotFoundException;
import com.example.tradesystem.merchant.domain.model.MerchantAccount;
import com.example.tradesystem.merchant.domain.model.ProductInventory;
import com.example.tradesystem.merchant.infrastructure.mapper.MerchantAccountMapper;
import com.example.tradesystem.merchant.infrastructure.mapper.ProductInventoryMapper;
import com.example.tradesystem.user.domain.model.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * 商家服务
 */
@Service
@Slf4j
public class MerchantService {

    @Autowired
    private ProductInventoryMapper productInventoryMapper;

    @Autowired
    private MerchantAccountMapper merchantAccountMapper;

    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    /**
     * 添加/更新库存
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductInventory addStock(Long merchantId, String sku, Integer quantity, BigDecimal price, String productName) {
        log.info("开始添加库存: merchantId={}, sku={}, quantity={}, price={}", merchantId, sku, quantity, price);

        // 1. 验证SKU格式
        if (!SKU_PATTERN.matcher(sku).matches()) {
            throw new BusinessException(400, "SKU格式不正确，只能包含大写字母、数字和下划线");
        }

        // 2. 验证参数
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "库存数量必须大于0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "商品价格必须大于0");
        }

        // 3. 查询或创建库存记录
        ProductInventory inventory = productInventoryMapper.selectBySkuForUpdate(merchantId, sku);
        
        Money moneyPrice = new Money(price);
        
        if (inventory == null) {
            // 创建新库存
            inventory = new ProductInventory(sku, merchantId, productName, moneyPrice, quantity);
            productInventoryMapper.insert(inventory);
            log.info("创建新商品库存: sku={}, stock={}", sku, quantity);
        } else {
            // 累加库存
            inventory.addStock(quantity);
            // 可选: 更新价格
            if (price != null) {
                inventory.updatePrice(moneyPrice);
            }
            int updatedRows = productInventoryMapper.updateWithVersion(inventory);
            if (updatedRows == 0) {
                throw new BusinessException("更新库存失败，可能存在并发冲突");
            }
            log.info("更新商品库存: sku={}, newStock={}", sku, inventory.getStockQuantity());
        }

        return inventory;
    }

    /**
     * 查询商品信息
     */
    public ProductInventory getProductInfo(Long merchantId, String sku) {
        ProductInventory inventory = productInventoryMapper.selectBySku(merchantId, sku);
        if (inventory == null) {
            throw new ResourceNotFoundException("商品不存在");
        }
        return inventory;
    }

    /**
     * 查询商家账户
     */
    public MerchantAccount getMerchantAccount(Long merchantId) {
        MerchantAccount account = merchantAccountMapper.selectById(merchantId);
        if (account == null) {
            throw new ResourceNotFoundException("商家不存在");
        }
        return account;
    }

    /**
     * 扣减库存(供事件监听器调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(String orderId, Long merchantId, String sku, Integer quantity) {
        log.info("开始扣减库存: orderId={}, merchantId={}, sku={}, quantity={}", 
            orderId, merchantId, sku, quantity);

        int maxRetries = 3;
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                ProductInventory inventory = productInventoryMapper.selectBySku(merchantId, sku);
                if (inventory == null) {
                    throw new ResourceNotFoundException("商品不存在: " + sku);
                }

                // 先验证库存是否充足（业务校验，不需要重试）
                if (!inventory.isStockSufficient(quantity)) {
                    throw new com.example.tradesystem.common.exception.InsufficientStockException(
                        "库存不足，当前库存: " + inventory.getStockQuantity()
                    );
                }

                inventory.deductStock(quantity);

                int updatedRows = productInventoryMapper.deductStockWithVersion(
                    merchantId,
                    sku,
                    quantity,
                    inventory.getVersion()
                );

                if (updatedRows == 0) {
                    if (retryCount < maxRetries - 1) {
                        long waitTime = (long) Math.pow(2, retryCount) * 100;
                        log.warn("库存扣减失败（乐观锁冲突），{}ms后重试: orderId={}", waitTime, orderId);
                        Thread.sleep(waitTime);
                        continue;
                    } else {
                        throw new com.example.tradesystem.common.exception.OptimisticLockException(
                            "库存扣减失败，并发冲突，已重试" + maxRetries + "次"
                        );
                    }
                }

                log.info("库存扣减成功: sku={}, orderId={}", sku, orderId);
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("库存扣减被中断", e);
            } catch (com.example.tradesystem.common.exception.InsufficientStockException e) {
                // 库存不足是业务异常，不需要重试，直接抛出
                log.error("库存不足: orderId={}, sku={}, required={}, current={}", 
                    orderId, sku, quantity, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("库存扣减失败: orderId={}, retry={}/{}", orderId, retryCount + 1, maxRetries, e);
                if (retryCount == maxRetries - 1) {
                    throw e;
                }
            }
        }
    }

    /**
     * 恢复库存(供事件监听器调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(String orderId, Long merchantId, String sku, Integer quantity) {
        log.info("开始恢复库存: orderId={}, merchantId={}, sku={}, quantity={}", 
            orderId, merchantId, sku, quantity);

        ProductInventory inventory = productInventoryMapper.selectBySku(merchantId, sku);
        if (inventory == null) {
            throw new ResourceNotFoundException("商品不存在: " + sku);
        }

        inventory.restoreStock(quantity);

        int updatedRows = productInventoryMapper.restoreStockWithVersion(
            merchantId,
            sku,
            quantity,
            inventory.getVersion()
        );

        if (updatedRows == 0) {
            throw new com.example.tradesystem.common.exception.OptimisticLockException("恢复库存失败，并发冲突");
        }

        log.info("库存恢复成功: sku={}, orderId={}", sku, orderId);
    }

    /**
     * 增加商家余额(供事件监听器调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void creditMerchant(String orderId, Long merchantId, BigDecimal amount) {
        log.info("开始增加商家余额: orderId={}, merchantId={}, amount={}", orderId, merchantId, amount);

        MerchantAccount account = merchantAccountMapper.selectById(merchantId);
        if (account == null) {
            // 自动创建商家账户
            account = new MerchantAccount(merchantId, new Money(BigDecimal.ZERO));
            merchantAccountMapper.insert(account);
            log.info("创建新商家账户: merchantId={}", merchantId);
        }

        Money creditAmount = new Money(amount);
        account.credit(creditAmount);

        int updatedRows = merchantAccountMapper.increaseBalanceWithVersion(
            merchantId,
            creditAmount.getAmount(),
            account.getVersion()
        );

        if (updatedRows == 0) {
            throw new com.example.tradesystem.common.exception.OptimisticLockException("增加商家余额失败，并发冲突");
        }

        log.info("商家余额增加成功: merchantId={}, orderId={}", merchantId, orderId);
    }
}
