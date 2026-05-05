package com.example.tradesystem.merchant.application.service;

import com.example.tradesystem.merchant.domain.model.SettlementRecord;
import com.example.tradesystem.merchant.infrastructure.mapper.MerchantCreditRecordMapper;
import com.example.tradesystem.merchant.infrastructure.mapper.SettlementRecordMapper;
import com.example.tradesystem.user.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SettlementService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("对账服务测试")
class SettlementServiceTest {

    @Mock
    private MerchantCreditRecordMapper creditRecordMapper;

    @Mock
    private SettlementRecordMapper settlementRecordMapper;

    @InjectMocks
    private SettlementService settlementService;

    private LocalDate yesterday;
    private String dateStr;
    private String startDateTime;
    private String endDateTime;

    @BeforeEach
    void setUp() {
        // 初始化昨天的日期
        yesterday = LocalDate.now().minusDays(1);
        dateStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);
        startDateTime = dateStr + " 00:00:00";
        endDateTime = dateStr + " 23:59:59";
    }

    @Test
    @DisplayName("测试每日对账 - 正常流程，两个商家对账一致")
    void testDailySettlement_Success_AllMatched() {
        // Given - 准备测试数据
        Long merchantId1 = 2001L;
        Long merchantId2 = 2002L;
        BigDecimal amount1 = new BigDecimal("1000.00");
        BigDecimal amount2 = new BigDecimal("2000.00");

        // Mock Mapper 返回值
        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId1), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount1);
        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId2), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount2);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When - 执行测试
        settlementService.dailySettlement();

        // Then - 验证结果
        // 验证查询收款总额被调用了2次（每个商家一次）
        verify(creditRecordMapper, times(2)).sumAmountByDateRange(anyLong(), anyString(), anyString());
        
        // 验证插入对账记录被调用了2次
        verify(settlementRecordMapper, times(2)).insert(any(SettlementRecord.class));

        // 捕获插入的对账记录并验证
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, times(2)).insert(captor.capture());

        java.util.List<SettlementRecord> capturedRecords = captor.getAllValues();
        assertEquals(2, capturedRecords.size());

        // 验证第一个商家的对账记录
        SettlementRecord record1 = capturedRecords.get(0);
        assertEquals(merchantId1, record1.getMerchantId());
        assertEquals(yesterday, record1.getSettlementDate());
        assertTrue(record1.isMatched());
        assertEquals(0, record1.getDifference().getAmount().compareTo(BigDecimal.ZERO));

        // 验证第二个商家的对账记录
        SettlementRecord record2 = capturedRecords.get(1);
        assertEquals(merchantId2, record2.getMerchantId());
        assertEquals(yesterday, record2.getSettlementDate());
        assertTrue(record2.isMatched());
        assertEquals(0, record2.getDifference().getAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("测试每日对账 - 对账不一致场景")
    void testDailySettlement_Mismatched() {
        // Given
        Long merchantId = 2001L;
        BigDecimal totalSales = new BigDecimal("1000.00");
        BigDecimal totalReceived = new BigDecimal("950.00");
        BigDecimal difference = new BigDecimal("50.00");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(totalReceived);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, atLeastOnce()).insert(captor.capture());

        SettlementRecord record = captor.getValue();
        assertFalse(record.isMatched());
        assertEquals(0, record.getDifference().getAmount().compareTo(difference));
    }

    @Test
    @DisplayName("测试每日对账 - 收款金额为0")
    void testDailySettlement_ZeroAmount() {
        // Given
        Long merchantId = 2001L;
        BigDecimal zeroAmount = BigDecimal.ZERO;

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(zeroAmount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, atLeastOnce()).insert(captor.capture());

        SettlementRecord record = captor.getValue();
        assertTrue(record.isMatched());
        assertTrue(record.getTotalSales().isZero());
        assertTrue(record.getTotalReceived().isZero());
    }

    @Test
    @DisplayName("测试每日对账 - Mapper查询返回null")
    void testDailySettlement_NullAmount() {
        // Given
        Long merchantId = 2001L;

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(null);

        // When & Then - 应该抛出异常
        assertThrows(Exception.class, () -> {
            settlementService.dailySettlement();
        });
    }

    @Test
    @DisplayName("测试每日对账 - 数据库插入失败")
    void testDailySettlement_InsertFailure() {
        // Given
        Long merchantId = 2001L;
        BigDecimal amount = new BigDecimal("1000.00");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class)))
                .thenThrow(new RuntimeException("数据库插入失败"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            settlementService.dailySettlement();
        });

        // 验证至少尝试了一次插入
        verify(settlementRecordMapper, atLeastOnce()).insert(any(SettlementRecord.class));
    }

    @Test
    @DisplayName("测试单个商家对账 - 验证对账ID格式")
    void testPerformSettlement_SettlementIdFormat() {
        // Given
        Long merchantId = 2001L;
        BigDecimal amount = new BigDecimal("1500.00");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, atLeastOnce()).insert(captor.capture());

        SettlementRecord record = captor.getValue();
        assertNotNull(record.getSettlementId());
        assertTrue(record.getSettlementId().startsWith("SET_"));
        assertEquals(24, record.getSettlementId().length()); // SET_ + 20字符
    }

    @Test
    @DisplayName("测试对账记录的金额计算正确性")
    void testSettlementRecord_AmountCalculation() {
        // Given
        Long merchantId = 2001L;
        BigDecimal salesAmount = new BigDecimal("5000.00");
        BigDecimal receivedAmount = new BigDecimal("4800.00");
        BigDecimal expectedDifference = new BigDecimal("200.00");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(receivedAmount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, atLeastOnce()).insert(captor.capture());

        SettlementRecord record = captor.getValue();
        assertEquals(0, record.getTotalSales().getAmount().compareTo(receivedAmount));
        assertEquals(0, record.getTotalReceived().getAmount().compareTo(receivedAmount));
        assertEquals(0, record.getDifference().getAmount().compareTo(expectedDifference));
    }

    @Test
    @DisplayName("测试每日对账 - 验证日期参数正确性")
    void testDailySettlement_DateParameters() {
        // Given
        Long merchantId = 2001L;
        BigDecimal amount = new BigDecimal("1000.00");

        when(creditRecordMapper.sumAmountByDateRange(anyLong(), anyString(), anyString()))
                .thenReturn(amount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then - 验证日期格式正确
        verify(creditRecordMapper, atLeastOnce()).sumAmountByDateRange(
                eq(merchantId),
                contains(dateStr),
                contains(dateStr)
        );
    }

    @Test
    @DisplayName("测试每日对账 - 多个商家独立处理")
    void testDailySettlement_MultipleMerchantsIndependent() {
        // Given
        Long merchantId1 = 2001L;
        Long merchantId2 = 2002L;
        BigDecimal amount1 = new BigDecimal("1000.00");
        BigDecimal amount2 = new BigDecimal("2000.00");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId1), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount1);
        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId2), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount2);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then - 验证每个商家都被独立处理
        verify(creditRecordMapper).sumAmountByDateRange(eq(merchantId1), eq(startDateTime), eq(endDateTime));
        verify(creditRecordMapper).sumAmountByDateRange(eq(merchantId2), eq(startDateTime), eq(endDateTime));
        
        // 验证插入了两条对账记录
        verify(settlementRecordMapper, times(2)).insert(any(SettlementRecord.class));
    }

    @Test
    @DisplayName("测试每日对账 - 验证Money对象创建")
    void testDailySettlement_MoneyObjectCreation() {
        // Given
        Long merchantId = 2001L;
        BigDecimal amount = new BigDecimal("999.99");

        when(creditRecordMapper.sumAmountByDateRange(eq(merchantId), eq(startDateTime), eq(endDateTime)))
                .thenReturn(amount);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        // When
        settlementService.dailySettlement();

        // Then
        ArgumentCaptor<SettlementRecord> captor = ArgumentCaptor.forClass(SettlementRecord.class);
        verify(settlementRecordMapper, atLeastOnce()).insert(captor.capture());

        SettlementRecord record = captor.getValue();
        assertNotNull(record.getTotalSales());
        assertNotNull(record.getTotalReceived());
        assertNotNull(record.getDifference());
        
        // 验证金额精度（保留2位小数）
        assertEquals(2, record.getTotalSales().getAmount().scale());
        assertEquals(2, record.getTotalReceived().getAmount().scale());
    }
}
