package com.yongquan.propertysaas.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.ReconcileSummaryView;
import com.yongquan.propertysaas.payment.domain.RefundNotifyResult;
import com.yongquan.propertysaas.payment.dto.RefundAuditRequest;
import com.yongquan.propertysaas.payment.dto.RefundCreateRequest;
import com.yongquan.propertysaas.payment.dto.WechatRefundNotifyRequest;
import com.yongquan.propertysaas.payment.repository.PaymentRefundRepository;
import com.yongquan.propertysaas.payment.wechat.WechatPayClient;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRefundService {

    private static final Set<String> REFUND_STATUSES = Set.of(
            "APPLYING", "AUDIT_PASSED", "AUDIT_REJECTED", "REFUNDING", "REFUNDED", "FAILED", "CLOSED");

    private final PaymentRefundRepository repository;
    private final OperationLogService operationLogService;
    private final WechatPayClient wechatPayClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public PaymentRefundService(PaymentRefundRepository repository, OperationLogService operationLogService,
                                WechatPayClient wechatPayClient, ObjectMapper objectMapper) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.wechatPayClient = wechatPayClient;
        this.objectMapper = objectMapper;
    }

    public PageResult<PayRefundView> pageRefunds(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateRefundStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findRefunds(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countRefunds(tenantId, scope, projectId, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createRefund(RefundCreateRequest request) {
        ensureProjectAllowed(request.projectId());
        PayOrderView order = repository.getOrder(tenantId(), request.orderId());
        if (!request.projectId().equals(order.projectId())) {
            throw new IllegalArgumentException("订单不属于项目：" + request.orderId());
        }
        if (!Set.of("PAID", "PARTIAL_REFUNDED").contains(order.status())) {
            throw new IllegalArgumentException("当前订单状态不可退款：" + order.status());
        }
        BigDecimal refundAmount = money(request.refundAmount());
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }
        BigDecimal refundable = money(repository.refundableAmount(tenantId(), order.orderId()));
        if (refundAmount.compareTo(refundable) > 0) {
            throw new IllegalArgumentException("退款金额超过可退金额");
        }
        Long refundId = newId();
        String refundNo = "REF-" + tenantId() + "-" + request.projectId() + "-" + refundId;
        Long transactionId = repository.findTransactionId(tenantId(), order.orderId());
        repository.insertRefund(tenantId(), request.projectId(), refundId, refundNo, order.orderId(), transactionId,
                refundAmount, request.reason(), userId());
        operationLogService.record(new OperationLogWrite(tenantId(), request.projectId(), "payment", "REFUND_APPLY",
                "pay_refund", refundId, Map.of("orderId", order.orderId(), "orderStatus", order.status()),
                Map.of("refundNo", refundNo, "status", "APPLYING", "refundAmount", refundAmount), request.reason()));
        return refundId;
    }

    @Transactional
    public void auditRefund(Long refundId, RefundAuditRequest request) {
        PayRefundView refund = repository.getRefund(tenantId(), refundId);
        ensureProjectAllowed(refund.projectId());
        String status = switch (request.auditResult()) {
            case "APPROVED" -> "REFUNDING";
            case "REJECTED" -> "AUDIT_REJECTED";
            default -> throw new IllegalArgumentException("非法退款审批结果：" + request.auditResult());
        };
        int updated = repository.auditRefund(tenantId(), refundId, status, userId());
        if (updated == 0) {
            throw new IllegalArgumentException("退款单状态已变化，审批失败");
        }
        operationLogService.record(new OperationLogWrite(tenantId(), refund.projectId(), "payment", "REFUND_AUDIT",
                "pay_refund", refundId, Map.of("status", refund.status()),
                Map.of("status", status, "auditResult", request.auditResult()), request.auditRemark()));
    }

    @Transactional
    public RefundNotifyResult handleWechatRefundNotify(WechatRefundNotifyRequest request) {
        PayRefundView refund = repository.getRefundByNo(request.refundNo());
        Long tenantId = refundTenantId(refund.refundNo());
        if ("REFUNDED".equals(refund.status())) {
            return new RefundNotifyResult(refund.refundNo(), "REFUNDED", true);
        }
        if (!"REFUNDING".equals(refund.status())) {
            throw new IllegalArgumentException("退款单状态不可回调：" + refund.status());
        }
        verifyWechatRefundSignature(tenantId, refund, request);
        BigDecimal notifyAmount = money(request.refundAmount());
        if (notifyAmount.compareTo(refund.refundAmount()) != 0) {
            throw new IllegalArgumentException("退款回调金额与退款单金额不一致");
        }
        if (repository.refundTransactionExists(tenantId, "WECHAT", request.thirdRefundNo())) {
            repository.markRefundRefunded(tenantId, refund.refundId(), request.thirdRefundNo(), request.refundedAt(), toJson(request));
            return new RefundNotifyResult(refund.refundNo(), "REFUNDED", true);
        }
        String rawNotify = toJson(request);
        boolean inserted = repository.insertRefundTransactionIfAbsent(newId(), tenantId, refund.projectId(),
                refund.refundId(), refund.refundNo(), request.thirdRefundNo(), "WECHAT", notifyAmount,
                request.refundedAt(), rawNotify);
        if (!inserted) {
            repository.markRefundRefunded(tenantId, refund.refundId(), request.thirdRefundNo(), request.refundedAt(), rawNotify);
            return new RefundNotifyResult(refund.refundNo(), "REFUNDED", true);
        }
        allocateRefundToBills(tenantId, refund.orderId(), notifyAmount);
        repository.markRefundRefunded(tenantId, refund.refundId(), request.thirdRefundNo(), request.refundedAt(), rawNotify);
        repository.markOrderRefundStatus(tenantId, refund.orderId());
        return new RefundNotifyResult(refund.refundNo(), "REFUNDED", false);
    }

    public ReconcileSummaryView reconcile(Long projectId) {
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        return repository.reconcile(tenantId(), projectScope(tenantId()), projectId);
    }

    private void allocateRefundToBills(Long tenantId, Long orderId, BigDecimal refundAmount) {
        BigDecimal remaining = refundAmount;
        for (PayableBill bill : repository.findOrderBills(tenantId, orderId)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            BigDecimal amount = bill.remainingAmount().min(remaining).setScale(2, RoundingMode.HALF_UP);
            repository.refundBill(tenantId, bill.billId(), amount);
            remaining = remaining.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private void verifyWechatRefundSignature(Long tenantId, PayRefundView refund, WechatRefundNotifyRequest request) {
        String secret = repository.findPaySecret(tenantId, refund.projectId(), "WECHAT");
        wechatPayClient.verifyRefundNotify(tenantId, refund, secret, request);
    }

    private void validateRefundStatus(String status) {
        if (status != null && !status.isBlank() && !REFUND_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法退款状态：" + status);
        }
    }

    private void ensureProjectAllowed(Long projectId) {
        if (!repository.projectExists(tenantId(), projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("退款回调报文无法序列化", ex);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long refundTenantId(String refundNo) {
        String[] parts = refundNo.split("-");
        if (parts.length < 4) {
            throw new IllegalArgumentException("退款单号格式错误：" + refundNo);
        }
        return Long.parseLong(parts[1]);
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
