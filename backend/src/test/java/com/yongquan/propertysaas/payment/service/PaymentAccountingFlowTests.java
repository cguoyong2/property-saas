package com.yongquan.propertysaas.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yongquan.propertysaas.payment.domain.OrderBillSettlement;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.payment.dto.RefundAuditRequest;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.repository.PaymentRefundRepository;
import com.yongquan.propertysaas.payment.repository.PaymentRepository;
import com.yongquan.propertysaas.payment.wechat.WechatPayClient;
import com.yongquan.propertysaas.service.service.AppMessageService;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentAccountingFlowTests {

    private static final Long TENANT_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 1001L;
    private static final Long MEMBER_ID = 2001L;
    private static final Long BILL_ID = 3001L;
    private static final Long ORDER_ID = 4001L;
    private static final Long REFUND_ID = 5001L;

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(USER_ID, TENANT_ID, null, "TENANT");
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void offlinePartialCollectionKeepsRemainingBillAndDoesNotCreatePrepayment() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentService service = new PaymentService(repository, mock(WechatPayClient.class),
                objectMapper(), mock(AppMessageService.class));
        when(repository.projectExists(TENANT_ID, PROJECT_ID)).thenReturn(true);
        when(repository.findAllowedProjectIds(TENANT_ID, USER_ID)).thenReturn(null);
        when(repository.findPayableBills(TENANT_ID, PROJECT_ID, List.of(BILL_ID)))
                .thenReturn(List.of(payableBill(BILL_ID, "BILL-001", new BigDecimal("100.00"))));
        when(repository.insertTransactionIfAbsent(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), anyLong(),
                anyString(), anyString(), eq("CASH"), eq(new BigDecimal("40.00")), any(LocalDateTime.class), anyString()))
                .thenReturn(true);
        when(repository.findOrderBillSettlements(eq(TENANT_ID), anyLong()))
                .thenReturn(List.of(new OrderBillSettlement(BILL_ID, MEMBER_ID,
                        new BigDecimal("40.00"), new BigDecimal("100.00"))));
        when(repository.settleBill(TENANT_ID, BILL_ID, new BigDecimal("40.00"))).thenReturn(1);
        when(repository.getOrderByNo(anyString())).thenReturn(order("PAY-TEST", "CASH", new BigDecimal("40.00"), "PAID"));

        service.collectOffline(new PayOrderCreateRequest(PROJECT_ID, List.of(BILL_ID), "CASH", new BigDecimal("40.00")));

        verify(repository).insertOrderBill(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), anyLong(),
                eq(BILL_ID), eq(new BigDecimal("40.00")));
        verify(repository).settleBill(TENANT_ID, BILL_ID, new BigDecimal("40.00"));
        verify(repository, never()).insertPrepayment(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
                anyString(), any(BigDecimal.class), anyString(), anyLong(), anyString());
    }

    @Test
    void wechatQrOverpaySettlesBillAndTransfersExtraAmountToPrepayment() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentService service = new PaymentService(repository, mock(WechatPayClient.class),
                objectMapper(), mock(AppMessageService.class));
        when(repository.projectExists(TENANT_ID, PROJECT_ID)).thenReturn(true);
        when(repository.findAllowedProjectIds(TENANT_ID, USER_ID)).thenReturn(null);
        when(repository.findPaySecret(TENANT_ID, PROJECT_ID, "WECHAT")).thenReturn("secret");
        when(repository.findPayableBills(TENANT_ID, PROJECT_ID, List.of(BILL_ID)))
                .thenReturn(List.of(payableBill(BILL_ID, "BILL-002", new BigDecimal("100.00"))));
        when(repository.getOrderByNo("PAY-QR-001"))
                .thenReturn(order("PAY-QR-001", "WECHAT", new BigDecimal("150.00"), "PAYING"));
        when(repository.transactionExists(TENANT_ID, "WECHAT", "WX-TX-001")).thenReturn(false);
        when(repository.insertTransactionIfAbsent(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), eq(ORDER_ID),
                eq("PAY-QR-001"), eq("WX-TX-001"), eq("WECHAT"), eq(new BigDecimal("150.00")),
                any(LocalDateTime.class), anyString())).thenReturn(true);
        when(repository.findOrderBillSettlements(TENANT_ID, ORDER_ID))
                .thenReturn(List.of(new OrderBillSettlement(BILL_ID, MEMBER_ID,
                        new BigDecimal("100.00"), new BigDecimal("100.00"))));
        when(repository.settleBill(TENANT_ID, BILL_ID, new BigDecimal("100.00"))).thenReturn(1);

        service.handleWechatNotify(new WechatPayNotifyRequest(
                "PAY-QR-001",
                "WX-TX-001",
                new BigDecimal("150.00"),
                LocalDateTime.now(),
                "DEV_SIMULATED",
                Map.of("source", "unit-test")
        ));

        verify(repository).settleBill(TENANT_ID, BILL_ID, new BigDecimal("100.00"));
        verify(repository).insertPrepayment(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), eq(MEMBER_ID), eq(ORDER_ID),
                eq("PAY-QR-001"), eq(new BigDecimal("50.00")), eq("WECHAT_OVERPAY"), eq(USER_ID),
                eq("收款码收款超出应收金额，自动转入预存款"));
        verify(repository).markOrderPaid(eq(TENANT_ID), eq(ORDER_ID), eq("WX-TX-001"), any(LocalDateTime.class));
    }

    @Test
    void refundCompletionRefundsBillsBeforeRemainingPrepayment() {
        PaymentRefundRepository repository = mock(PaymentRefundRepository.class);
        PaymentRefundService service = new PaymentRefundService(repository, mock(OperationLogService.class),
                mock(WechatPayClient.class), objectMapper(), mock(AppMessageService.class));
        when(repository.projectExists(TENANT_ID, PROJECT_ID)).thenReturn(true);
        when(repository.findAllowedProjectIds(TENANT_ID, USER_ID)).thenReturn(null);
        when(repository.getRefund(TENANT_ID, REFUND_ID)).thenReturn(refund(new BigDecimal("150.00"), "REFUNDING"));
        when(repository.insertRefundTransactionIfAbsent(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), eq(REFUND_ID),
                eq("REF-001"), eq("OFFLINE-REF-001"), eq("OFFLINE"), eq(new BigDecimal("150.00")),
                any(LocalDateTime.class), anyString())).thenReturn(true);
        when(repository.findOrderBills(TENANT_ID, ORDER_ID))
                .thenReturn(List.of(payableBill(BILL_ID, "BILL-003", new BigDecimal("100.00"))));
        when(repository.refundPrepayments(TENANT_ID, ORDER_ID, new BigDecimal("50.00")))
                .thenReturn(new BigDecimal("50.00"));

        service.confirmOfflineRefund(REFUND_ID, new RefundAuditRequest("APPROVED", "线下退款确认"));

        verify(repository).refundBill(TENANT_ID, BILL_ID, new BigDecimal("100.00"));
        verify(repository).refundPrepayments(TENANT_ID, ORDER_ID, new BigDecimal("50.00"));
        verify(repository).markRefundRefunded(eq(TENANT_ID), eq(REFUND_ID), eq("OFFLINE-REF-001"),
                any(LocalDateTime.class), anyString());
        verify(repository).markOrderRefundStatus(TENANT_ID, ORDER_ID);
    }

    @Test
    void refundCompletionRejectsAmountBeyondCurrentRefundableAssets() {
        PaymentRefundRepository repository = mock(PaymentRefundRepository.class);
        PaymentRefundService service = new PaymentRefundService(repository, mock(OperationLogService.class),
                mock(WechatPayClient.class), objectMapper(), mock(AppMessageService.class));
        when(repository.projectExists(TENANT_ID, PROJECT_ID)).thenReturn(true);
        when(repository.findAllowedProjectIds(TENANT_ID, USER_ID)).thenReturn(null);
        when(repository.getRefund(TENANT_ID, REFUND_ID)).thenReturn(refund(new BigDecimal("150.00"), "REFUNDING"));
        when(repository.insertRefundTransactionIfAbsent(anyLong(), eq(TENANT_ID), eq(PROJECT_ID), eq(REFUND_ID),
                eq("REF-001"), eq("OFFLINE-REF-001"), eq("OFFLINE"), eq(new BigDecimal("150.00")),
                any(LocalDateTime.class), anyString())).thenReturn(true);
        when(repository.findOrderBills(TENANT_ID, ORDER_ID))
                .thenReturn(List.of(payableBill(BILL_ID, "BILL-004", new BigDecimal("100.00"))));
        when(repository.refundPrepayments(TENANT_ID, ORDER_ID, new BigDecimal("50.00")))
                .thenReturn(BigDecimal.ZERO.setScale(2));

        assertThatThrownBy(() -> service.confirmOfflineRefund(REFUND_ID,
                new RefundAuditRequest("APPROVED", "线下退款确认")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("退款金额超过订单当前可退账单和预存款余额");
    }

    private PayableBill payableBill(Long billId, String billNo, BigDecimal remainingAmount) {
        return new PayableBill(billId, PROJECT_ID, billNo, MEMBER_ID, remainingAmount, "UNPAID");
    }

    private PayOrderView order(String orderNo, String payChannel, BigDecimal amount, String status) {
        return new PayOrderView(ORDER_ID, TENANT_ID, PROJECT_ID, orderNo, MEMBER_ID, payChannel, amount,
                "测试订单", status, LocalDateTime.now().plusMinutes(30), null, null, LocalDateTime.now(),
                "测试业主", "13900000001", "1栋1单元101", "测试账单", 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, amount, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private PayRefundView refund(BigDecimal refundAmount, String status) {
        return new PayRefundView(REFUND_ID, PROJECT_ID, "REF-001", ORDER_ID, "PAY-001", 6001L,
                refundAmount, "测试退款", status, null, null, USER_ID, null, null, LocalDateTime.now(),
                MEMBER_ID, "测试业主", "13900000001", "1栋1单元101", "测试账单", "OFFLINE",
                new BigDecimal("150.00"), BigDecimal.ZERO);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
