package com.example.tradesystem.merchant.application.service;

import com.example.tradesystem.merchant.domain.model.SettlementRecord;
import com.example.tradesystem.merchant.infrastructure.mapper.MerchantCreditRecordMapper;
import com.example.tradesystem.merchant.infrastructure.mapper.SettlementRecordMapper;
import com.example.tradesystem.user.domain.model.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 对账服务 - T+1自动对账
 */
@Service
@Slf4j
public class SettlementService {

    @Autowired
    private MerchantCreditRecordMapper creditRecordMapper;

    @Autowired
    private SettlementRecordMapper settlementRecordMapper;

    /**
     * 每日凌晨2点执行对账
     */
    @Scheduled(cron = "${settlement.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void dailySettlement() {
        log.info("开始执行每日对账任务");

        try {
            // 对账日期为昨天
            LocalDate settlementDate = LocalDate.now().minusDays(1);
            String dateStr = settlementDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // 这里简化处理,实际应该遍历所有商家
            Long[] merchantIds = {2001L, 2002L};  // 示例商家ID
            
            for (Long merchantId : merchantIds) {
                performSettlement(merchantId, settlementDate, dateStr);
            }

            log.info("每日对账任务完成");

        } catch (Exception e) {
            log.error("每日对账任务失败", e);
            throw e;
        }
    }

    /**
     * 执行单个商家的对账
     */
    private void performSettlement(Long merchantId, LocalDate settlementDate, String dateStr) {
        log.info("开始对账: merchantId={}, date={}", merchantId, dateStr);

        try {
            // 1. 查询昨天的收款总额
            BigDecimal totalReceived = creditRecordMapper.sumAmountByDateRange(
                merchantId, 
                dateStr + " 00:00:00",
                dateStr + " 23:59:59"
            );

            // 防止数据库返回null，设置为0
            if (totalReceived == null) {
                totalReceived = BigDecimal.ZERO;
                log.warn("商家 {} 在日期 {} 没有收款记录，设置为0", merchantId, dateStr);
            }

            // 2. 计算理论销售额(这里简化处理,实际应该从订单表统计)
            BigDecimal totalSales = totalReceived;  // 假设一致

            // 3. 创建对账记录
            String settlementId = "SET_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            SettlementRecord settlementRecord = new SettlementRecord(
                settlementId,
                merchantId,
                settlementDate,
                new Money(totalSales),
                new Money(totalReceived)
            );

            settlementRecordMapper.insert(settlementRecord);

            if (settlementRecord.isMatched()) {
                log.info("对账一致: merchantId={}, amount={}", merchantId, totalReceived);
            } else {
                log.warn("对账不一致: merchantId={}, sales={}, received={}, difference={}", 
                    merchantId, totalSales, totalReceived, settlementRecord.getDifference());
            }

        } catch (Exception e) {
            log.error("对账失败: merchantId={}", merchantId, e);
            throw e;
        }
    }
}
