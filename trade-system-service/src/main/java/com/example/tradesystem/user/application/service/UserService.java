package com.example.tradesystem.user.application.service;

import com.example.tradesystem.common.exception.BusinessException;
import com.example.tradesystem.user.application.dto.request.RechargeRequest;
import com.example.tradesystem.user.application.dto.response.RechargeResponse;
import com.example.tradesystem.user.application.dto.response.UserBalanceResponse;
import com.example.tradesystem.user.domain.model.Money;
import com.example.tradesystem.user.domain.model.RechargeRecord;
import com.example.tradesystem.user.domain.model.UserAccount;
import com.example.tradesystem.user.infrastructure.mapper.RechargeRecordMapper;
import com.example.tradesystem.user.infrastructure.mapper.UserAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 用户服务
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    /**
     * 用户充值
     */
    @Transactional(rollbackFor = Exception.class)
    public RechargeResponse recharge(Long userId, RechargeRequest request) {
        log.info("开始充值: userId={}, amount={}, requestId={}", userId, request.getAmount(), request.getRequestId());

        try {
            // 1. 检查幂等性
            RechargeRecord existingRecord = rechargeRecordMapper.selectByRequestId(request.getRequestId());
            if (existingRecord != null) {
                log.info("重复请求，返回上次结果: requestId={}", request.getRequestId());
                UserAccount account = userAccountMapper.selectById(userId);
                return buildRechargeResponse(account, existingRecord.getRechargeId());
            }

            // 2. 查询或创建用户账户
            UserAccount account = userAccountMapper.selectForUpdate(userId);
            if (account == null) {
                account = new UserAccount(userId, new Money(BigDecimal.ZERO));
                userAccountMapper.insert(account);
                log.info("创建新用户账户: userId={}", userId);
            }

            // 3. 创建充值记录
            String rechargeId = generateRechargeId();
            Money amount = new Money(request.getAmount());
            RechargeRecord rechargeRecord = new RechargeRecord(rechargeId, userId, amount, request.getRequestId());
            rechargeRecordMapper.insert(rechargeRecord);

            // 4. 更新余额
            account.recharge(amount);
            int updatedRows = userAccountMapper.updateBalanceWithVersion(
                userId, 
                account.getBalance().getAmount(), 
                account.getVersion()
            );

            if (updatedRows == 0) {
                throw new BusinessException("更新余额失败，可能存在并发冲突");
            }

            log.info("充值成功: userId={}, rechargeId={}, newBalance={}", userId, rechargeId, account.getBalance());
            return buildRechargeResponse(account, rechargeId);

        } catch (DuplicateKeyException e) {
            log.warn("重复充值请求: requestId={}", request.getRequestId());
            UserAccount account = userAccountMapper.selectById(userId);
            RechargeRecord record = rechargeRecordMapper.selectByRequestId(request.getRequestId());
            return buildRechargeResponse(account, record != null ? record.getRechargeId() : null);
        } catch (IllegalArgumentException e) {
            log.error("充值参数错误: {}", e.getMessage());
            throw new BusinessException(400, e.getMessage());
        } catch (Exception e) {
            log.error("充值失败: userId={}", userId, e);
            throw new BusinessException("充值失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户余额
     */
    public UserBalanceResponse getBalance(Long userId) {
        UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new com.example.tradesystem.common.exception.ResourceNotFoundException("用户不存在");
        }
        return UserBalanceResponse.builder()
            .userId(account.getUserId())
            .balance(account.getBalance().getAmount())
            .currency(account.getCurrency())
            .build();
    }

    /**
     * 扣减用户余额(供事件监听器调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(String orderId, Long userId, BigDecimal amount) {
        log.info("开始扣减余额: orderId={}, userId={}, amount={}", orderId, userId, amount);
        
        int maxRetries = 3;
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                UserAccount account = userAccountMapper.selectById(userId);
                if (account == null) {
                    throw new com.example.tradesystem.common.exception.ResourceNotFoundException("用户不存在");
                }

                Money deductAmount = new Money(amount);
                
                // 先验证余额是否充足（业务校验，不需要重试）
                if (account.getBalance().compareTo(deductAmount) < 0) {
                    throw new com.example.tradesystem.common.exception.InsufficientBalanceException(
                        String.format("余额不足: 当前余额=%.2f, 需要金额=%.2f", 
                            account.getBalance().getAmount(), deductAmount.getAmount())
                    );
                }
                
                account.deduct(deductAmount);

                int updatedRows = userAccountMapper.deductBalanceWithVersion(
                    userId,
                    deductAmount.getAmount(),
                    account.getVersion()
                );

                if (updatedRows == 0) {
                    if (retryCount < maxRetries - 1) {
                        long waitTime = (long) Math.pow(2, retryCount) * 100;
                        log.warn("余额扣减失败（乐观锁冲突），{}ms后重试: orderId={}", waitTime, orderId);
                        Thread.sleep(waitTime);
                        continue;
                    } else {
                        throw new com.example.tradesystem.common.exception.OptimisticLockException(
                            "余额扣减失败，并发冲突，已重试" + maxRetries + "次"
                        );
                    }
                }

                log.info("余额扣减成功: userId={}, orderId={}", userId, orderId);
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("余额扣减被中断", e);
            } catch (com.example.tradesystem.common.exception.InsufficientBalanceException e) {
                // 余额不足是业务异常，不需要重试，直接抛出
                log.error("余额不足: orderId={}, userId={}, required={}, current={}", 
                    orderId, userId, amount, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("余额扣减失败: orderId={}, retry={}/{}", orderId, retryCount + 1, maxRetries, e);
                if (retryCount == maxRetries - 1) {
                    throw e;
                }
            }
        }
    }

    /**
     * 退款(供事件监听器调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderId, Long userId, BigDecimal amount) {
        log.info("开始退款: orderId={}, userId={}, amount={}", orderId, userId, amount);
        
        UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new com.example.tradesystem.common.exception.ResourceNotFoundException("用户不存在");
        }

        Money refundAmount = new Money(amount);
        account.refund(refundAmount);

        int updatedRows = userAccountMapper.increaseBalanceWithVersion(
            userId,
            refundAmount.getAmount(),
            account.getVersion()
        );

        if (updatedRows == 0) {
            throw new com.example.tradesystem.common.exception.OptimisticLockException("退款失败，并发冲突");
        }

        log.info("退款成功: userId={}, orderId={}", userId, orderId);
    }

    private String generateRechargeId() {
        return "RCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private RechargeResponse buildRechargeResponse(UserAccount account, String rechargeId) {
        return RechargeResponse.builder()
            .userId(account.getUserId())
            .balance(account.getBalance().getAmount())
            .rechargeId(rechargeId)
            .build();
    }
}
