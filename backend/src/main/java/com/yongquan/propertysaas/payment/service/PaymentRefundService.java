package com.yongquan.propertysaas.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionHistoryView;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionReviewView;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionView;
import com.yongquan.propertysaas.payment.domain.ReconcileSummaryView;
import com.yongquan.propertysaas.payment.domain.RefundableOrderView;
import com.yongquan.propertysaas.payment.dto.ReconcileExceptionHandleRequest;
import com.yongquan.propertysaas.payment.dto.ReconcileExceptionReviewRequest;
import com.yongquan.propertysaas.payment.domain.RefundNotifyResult;
import com.yongquan.propertysaas.payment.dto.RefundAuditRequest;
import com.yongquan.propertysaas.payment.dto.RefundCreateRequest;
import com.yongquan.propertysaas.payment.dto.WechatRefundNotifyRequest;
import com.yongquan.propertysaas.payment.repository.PaymentRefundRepository;
import com.yongquan.propertysaas.payment.wechat.WechatPayClient;
import com.yongquan.propertysaas.service.service.AppMessageService;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.EmptyResultDataAccessException;
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
    private final AppMessageService appMessageService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public PaymentRefundService(PaymentRefundRepository repository, OperationLogService operationLogService,
                                WechatPayClient wechatPayClient, ObjectMapper objectMapper,
                                AppMessageService appMessageService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.wechatPayClient = wechatPayClient;
        this.objectMapper = objectMapper;
        this.appMessageService = appMessageService;
    }

    public PageResult<PayRefundView> pageRefunds(Long projectId, String refundNo, String orderNo,
                                                  String memberName, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateRefundStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findRefunds(tenantId, scope, projectId, normalize(refundNo), normalize(orderNo),
                        normalize(memberName), normalize(status), offset(pageNo, pageSize), pageSize),
                repository.countRefunds(tenantId, scope, projectId, normalize(refundNo), normalize(orderNo),
                        normalize(memberName), normalize(status)),
                pageNo,
                pageSize);
    }

    public List<RefundableOrderView> refundableOrders(Long projectId, Long memberId) {
        if (projectId == null) {
            throw new IllegalArgumentException("请选择小区名称");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("请选择业主/住户");
        }
        ensureProjectAllowed(projectId);
        Long tenantId = tenantId();
        return repository.findRefundableOrders(tenantId, projectScope(tenantId), projectId, memberId);
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
        notifyRefund(order.tenantId(), order.projectId(), order.memberId(), "REFUND_APPLY",
                "退款申请已提交",
                "您的退款申请已提交。"
                        + "\n订单号：" + order.orderNo()
                        + "\n退款单号：" + refundNo
                        + "\n申请金额：" + refundAmount + " 元"
                        + (request.reason() == null || request.reason().isBlank() ? "" : "\n原因：" + request.reason()),
                refundVariables("orderNo", order.orderNo(), "refundNo", refundNo,
                        "refundAmount", refundAmount, "reason", request.reason()));
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
        notifyRefund(tenantId(), refund.projectId(), refund.memberId(), "REFUND_AUDIT",
                "APPROVED".equals(request.auditResult()) ? "退款审核通过" : "退款审核驳回",
                "您的退款申请已" + ("APPROVED".equals(request.auditResult()) ? "审核通过，等待退款到账。" : "被驳回。")
                        + "\n退款单号：" + refund.refundNo()
                        + "\n退款金额：" + refund.refundAmount() + " 元"
                        + (request.auditRemark() == null || request.auditRemark().isBlank() ? "" : "\n说明：" + request.auditRemark()),
                refundVariables("refundNo", refund.refundNo(), "refundAmount", refund.refundAmount(),
                        "auditResult", "APPROVED".equals(request.auditResult()) ? "审核通过" : "审核驳回",
                        "auditRemark", request.auditRemark()));
    }

    @Transactional
    public void confirmOfflineRefund(Long refundId, RefundAuditRequest request) {
        PayRefundView refund = repository.getRefund(tenantId(), refundId);
        ensureProjectAllowed(refund.projectId());
        if (!"REFUNDING".equals(refund.status())) {
            throw new IllegalArgumentException("只有审批通过待退款的单据才能确认退款完成");
        }
        String thirdRefundNo = "OFFLINE-" + refund.refundNo();
        LocalDateTime refundedAt = LocalDateTime.now();
        String rawNotify = toJson(Map.of(
                "source", "PC_OFFLINE_REFUND",
                "refundNo", refund.refundNo(),
                "auditRemark", request.auditRemark() == null ? "" : request.auditRemark()));
        boolean inserted = repository.insertRefundTransactionIfAbsent(newId(), tenantId(), refund.projectId(),
                refund.refundId(), refund.refundNo(), thirdRefundNo, "OFFLINE", refund.refundAmount(),
                refundedAt, rawNotify);
        if (inserted) {
            allocateRefundToBills(tenantId(), refund.orderId(), refund.refundAmount());
        }
        repository.markRefundRefunded(tenantId(), refund.refundId(), thirdRefundNo, refundedAt, rawNotify);
        repository.markOrderRefundStatus(tenantId(), refund.orderId());
        notifyRefund(tenantId(), refund.projectId(), refund.memberId(), "REFUND_SUCCESS",
                "退款已完成",
                "您的退款已完成。"
                        + "\n退款单号：" + refund.refundNo()
                        + "\n退款金额：" + refund.refundAmount() + " 元"
                        + "\n退款方式：线下退款",
                refundVariables("refundNo", refund.refundNo(), "refundAmount", refund.refundAmount(),
                        "refundChannel", "线下退款"));
        operationLogService.record(new OperationLogWrite(tenantId(), refund.projectId(), "payment", "OFFLINE_REFUND_CONFIRM",
                "pay_refund", refundId, Map.of("status", refund.status()),
                Map.of("status", "REFUNDED", "thirdRefundNo", thirdRefundNo, "refundAmount", refund.refundAmount()),
                request.auditRemark()));
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
            repository.markOrderRefundStatus(tenantId, refund.orderId());
            return new RefundNotifyResult(refund.refundNo(), "REFUNDED", true);
        }
        String rawNotify = toJson(request);
        boolean inserted = repository.insertRefundTransactionIfAbsent(newId(), tenantId, refund.projectId(),
                refund.refundId(), refund.refundNo(), request.thirdRefundNo(), "WECHAT", notifyAmount,
                request.refundedAt(), rawNotify);
        if (!inserted) {
            repository.markRefundRefunded(tenantId, refund.refundId(), request.thirdRefundNo(), request.refundedAt(), rawNotify);
            repository.markOrderRefundStatus(tenantId, refund.orderId());
            return new RefundNotifyResult(refund.refundNo(), "REFUNDED", true);
        }
        allocateRefundToBills(tenantId, refund.orderId(), notifyAmount);
        repository.markRefundRefunded(tenantId, refund.refundId(), request.thirdRefundNo(), request.refundedAt(), rawNotify);
        repository.markOrderRefundStatus(tenantId, refund.orderId());
        notifyRefund(tenantId, refund.projectId(), refund.memberId(), "REFUND_SUCCESS",
                "退款已完成",
                "您的退款已完成。"
                        + "\n退款单号：" + refund.refundNo()
                        + "\n退款金额：" + notifyAmount + " 元"
                        + "\n退款方式：微信",
                refundVariables("refundNo", refund.refundNo(), "refundAmount", notifyAmount,
                        "refundChannel", "微信"));
        return new RefundNotifyResult(refund.refundNo(), "REFUNDED", false);
    }

    public ReconcileSummaryView reconcile(Long projectId, String startDate, String endDate,
                                          String payChannel, String orderStatus) {
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        validatePayChannel(payChannel);
        validateOrderStatus(orderStatus);
        return repository.reconcile(tenantId(), projectScope(tenantId()), projectId,
                normalize(startDate), normalize(endDate), normalize(payChannel), normalize(orderStatus));
    }

    public PageResult<ReconcileExceptionView> pageReconcileExceptions(Long projectId, String exceptionType,
                                                                      String exceptionLevel,
                                                                      String businessNo, String memberName,
                                                                      String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        if (exceptionLevel != null && !exceptionLevel.isBlank()
                && !Set.of("高", "中", "低", "HIGH", "MEDIUM", "LOW").contains(exceptionLevel)) {
            throw new IllegalArgumentException("非法异常级别：" + exceptionLevel);
        }
        if (status != null && !status.isBlank() && !Set.of("OPEN", "HANDLED").contains(status)) {
            throw new IllegalArgumentException("非法处理状态：" + status);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findReconcileExceptions(tenantId, scope, projectId, normalize(exceptionType),
                        normalize(exceptionLevel), normalize(businessNo), normalize(memberName), normalize(status), offset(pageNo, pageSize), pageSize),
                repository.countReconcileExceptions(tenantId, scope, projectId, normalize(exceptionType),
                        normalize(exceptionLevel), normalize(businessNo), normalize(memberName), normalize(status)),
                pageNo,
                pageSize);
    }

    public PageResult<ReconcileExceptionReviewView> pageReconcileExceptionReviews(Long projectId, String exceptionType,
                                                                                  String memberName,
                                                                                  String reviewStatus,
                                                                                  String currentCheckStatus,
                                                                                  long pageNo,
                                                                                  long pageSize) {
        validatePage(pageNo, pageSize);
        if (reviewStatus != null && !reviewStatus.isBlank()
                && !Set.of("PENDING", "APPROVED", "REJECTED").contains(reviewStatus)) {
            throw new IllegalArgumentException("非法复核状态：" + reviewStatus);
        }
        if (currentCheckStatus != null && !currentCheckStatus.isBlank()
                && !Set.of("RESOLVED", "STILL_ABNORMAL").contains(currentCheckStatus)) {
            throw new IllegalArgumentException("非法复算状态：" + currentCheckStatus);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findReconcileExceptionReviews(tenantId, scope, projectId, normalize(exceptionType),
                        normalize(memberName), normalize(reviewStatus), normalize(currentCheckStatus),
                        offset(pageNo, pageSize), pageSize),
                repository.countReconcileExceptionReviews(tenantId, scope, projectId, normalize(exceptionType),
                        normalize(memberName), normalize(reviewStatus), normalize(currentCheckStatus)),
                pageNo,
                pageSize);
    }

    @Transactional
    public void handleReconcileException(String exceptionKey, ReconcileExceptionHandleRequest request) {
        ReconcileExceptionView exception = repository.getReconcileException(tenantId(), projectScope(tenantId()), exceptionKey);
        ensureProjectAllowed(exception.projectId());
        String attachmentFileIds = normalize(request.attachmentFileIds());
        if (isHighRiskReconcileException(exception.exceptionLevel()) && attachmentFileIds == null) {
            throw new IllegalArgumentException("高风险对账异常必须上传处理凭证后才能标记已处理");
        }
        String reviewStatus = requiresManualReconcileReview(exception.exceptionLevel()) ? "PENDING" : "APPROVED";
        String reviewRemark = "APPROVED".equals(reviewStatus) ? "低风险异常处理后自动归档" : null;
        repository.upsertReconcileExceptionHandle(newId(), tenantId(), exception.projectId(), exception.exceptionKey(),
                exception.exceptionType(), exception.businessType(), exception.businessId(), userId(),
                request.handleRemark(), attachmentFileIds, reviewStatus);
        repository.insertReconcileExceptionHistory(newId(), tenantId(), exception.projectId(), exception.exceptionKey(),
                "HANDLE", exception.status(), "HANDLED", exception.reviewStatus(), reviewStatus,
                request.handleRemark(), attachmentFileIds, userId());
        if ("APPROVED".equals(reviewStatus)) {
            repository.insertReconcileExceptionHistory(newId(), tenantId(), exception.projectId(), exception.exceptionKey(),
                    "AUTO_ARCHIVE", "HANDLED", "HANDLED", reviewStatus, reviewStatus,
                    reviewRemark, attachmentFileIds, userId());
        }
        operationLogService.record(new OperationLogWrite(tenantId(), exception.projectId(), "payment", "RECONCILE_EXCEPTION_HANDLE",
                exception.businessType(), exception.businessId(), Map.of("status", exception.status()),
                Map.of("status", "HANDLED", "exceptionKey", exception.exceptionKey(),
                        "reviewStatus", reviewStatus,
                        "attachmentFileIds", request.attachmentFileIds() == null ? "" : request.attachmentFileIds()),
                request.handleRemark()));
    }

    private boolean requiresManualReconcileReview(String exceptionLevel) {
        if (exceptionLevel == null) {
            return true;
        }
        String level = exceptionLevel.trim().toUpperCase();
        return !"低".equals(exceptionLevel.trim()) && !"LOW".equals(level);
    }

    private boolean isHighRiskReconcileException(String exceptionLevel) {
        if (exceptionLevel == null) {
            return false;
        }
        String level = exceptionLevel.trim().toUpperCase();
        return "高".equals(exceptionLevel.trim()) || "HIGH".equals(level) || "CRITICAL".equals(level);
    }

    @Transactional
    public void reviewReconcileException(String exceptionKey, ReconcileExceptionReviewRequest request) {
        String reviewStatus = switch (request.reviewResult()) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> throw new IllegalArgumentException("非法复核结果：" + request.reviewResult());
        };
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        ReconcileExceptionReviewView exception = repository.getReconcileExceptionReview(tenantId, scope, exceptionKey);
        ensureProjectAllowed(exception.projectId());
        if (!"HANDLED".equals(exception.status())) {
            throw new IllegalArgumentException("只有已处理的账务异常可以复核");
        }
        if ("APPROVED".equals(reviewStatus) && "STILL_ABNORMAL".equals(exception.currentCheckStatus())) {
            throw new IllegalArgumentException("处理后系统复算仍存在该异常，不能复核通过：" + exception.reason());
        }
        repository.reviewReconcileExceptionHandle(tenantId, exception.exceptionKey(), reviewStatus, userId(),
                request.reviewRemark());
        String newStatus = "APPROVED".equals(reviewStatus) ? "HANDLED" : "OPEN";
        repository.insertReconcileExceptionHistory(newId(), tenantId, exception.projectId(), exception.exceptionKey(),
                "APPROVED".equals(reviewStatus) ? "REVIEW_APPROVE" : "REVIEW_REJECT",
                exception.status(), newStatus, exception.reviewStatus(), reviewStatus, request.reviewRemark(),
                exception.attachmentFileIds(), userId());
        operationLogService.record(new OperationLogWrite(tenantId, exception.projectId(), "payment", "RECONCILE_EXCEPTION_REVIEW",
                exception.businessType(), exception.businessId(), Map.of("status", exception.status(),
                "reviewStatus", exception.reviewStatus()), Map.of("status", newStatus, "reviewStatus", reviewStatus,
                "exceptionKey", exception.exceptionKey()), request.reviewRemark()));
    }

    public PageResult<ReconcileExceptionHistoryView> pageReconcileExceptionHistory(String exceptionKey,
                                                                                   long pageNo,
                                                                                   long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        try {
            ReconcileExceptionReviewView handled = repository.getReconcileExceptionReview(tenantId, scope, exceptionKey);
            ensureProjectAllowed(handled.projectId());
        } catch (EmptyResultDataAccessException ignored) {
            ReconcileExceptionView current = repository.getReconcileException(tenantId, scope, exceptionKey);
            ensureProjectAllowed(current.projectId());
        }
        return new PageResult<>(
                repository.findReconcileExceptionHistory(tenantId, exceptionKey, offset(pageNo, pageSize), pageSize),
                repository.countReconcileExceptionHistory(tenantId, exceptionKey),
                pageNo,
                pageSize);
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

    private void validatePayChannel(String payChannel) {
        if (payChannel != null && !payChannel.isBlank()
                && !Set.of("WECHAT", "ALI", "OFFLINE", "POS", "CASH", "BANK_TRANSFER").contains(payChannel)) {
            throw new IllegalArgumentException("非法支付渠道：" + payChannel);
        }
    }

    private void validateOrderStatus(String status) {
        if (status != null && !status.isBlank()
                && !Set.of("PENDING", "PAYING", "PAID", "CLOSED", "FAILED", "REFUNDING", "REFUNDED", "PARTIAL_REFUNDED").contains(status)) {
            throw new IllegalArgumentException("非法订单状态：" + status);
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void notifyRefund(Long tenantId, Long projectId, Long memberId, String templateCode,
                              String title, String content) {
        appMessageService.sendToMember(tenantId, projectId, memberId, templateCode, title, content);
    }

    private void notifyRefund(Long tenantId, Long projectId, Long memberId, String templateCode,
                              String title, String content, Map<String, ?> variables) {
        appMessageService.sendToMember(tenantId, projectId, memberId, templateCode, title, content, variables);
    }

    private Map<String, Object> refundVariables(Object... values) {
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            variables.put(String.valueOf(values[i]), values[i + 1] == null ? "" : values[i + 1]);
        }
        return variables;
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
