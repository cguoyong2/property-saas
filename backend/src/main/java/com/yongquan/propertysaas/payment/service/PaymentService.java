package com.yongquan.propertysaas.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.MemberPrepaymentView;
import com.yongquan.propertysaas.payment.domain.OrderBillSettlement;
import com.yongquan.propertysaas.payment.domain.PayOrderCreateResult;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayTransactionView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.PaymentNotifyResult;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.repository.PaymentRepository;
import com.yongquan.propertysaas.payment.wechat.WechatPayClient;
import com.yongquan.propertysaas.service.service.AppMessageService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Set<String> PAY_CHANNELS = Set.of("WECHAT", "ALI", "OFFLINE", "POS", "CASH", "BANK_TRANSFER");
    private static final Set<String> OFFLINE_COLLECTION_CHANNELS = Set.of("OFFLINE", "POS", "CASH");
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING", "PAYING", "PAID", "CLOSED", "FAILED", "REFUNDING", "REFUNDED", "PARTIAL_REFUNDED");
    private static final Set<String> PAYABLE_BILL_STATUSES = Set.of("UNPAID", "OVERDUE", "PARTIAL_PAID");

    private final PaymentRepository repository;
    private final WechatPayClient wechatPayClient;
    private final ObjectMapper objectMapper;
    private final AppMessageService appMessageService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    private record BillAllocation(PayableBill bill, BigDecimal amount) {
    }

    private record PreparedPayment(List<BillAllocation> allocations, Long memberId, BigDecimal dueAmount,
                                   BigDecimal amount, BigDecimal prepaymentAmount) {
    }

    public PaymentService(PaymentRepository repository, WechatPayClient wechatPayClient, ObjectMapper objectMapper,
                          AppMessageService appMessageService) {
        this.repository = repository;
        this.wechatPayClient = wechatPayClient;
        this.objectMapper = objectMapper;
        this.appMessageService = appMessageService;
    }

    public PageResult<PayOrderView> pageOrders(Long projectId, String orderNo, String memberName,
                                               String payChannel, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (payChannel != null && !payChannel.isBlank()) {
            validatePayChannel(payChannel);
        }
        validateOrderStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findOrders(tenantId, scope, projectId, normalize(orderNo), normalize(memberName),
                        normalize(payChannel), normalize(status), offset(pageNo, pageSize), pageSize),
                repository.countOrders(tenantId, scope, projectId, normalize(orderNo), normalize(memberName),
                        normalize(payChannel), normalize(status)),
                pageNo,
                pageSize);
    }

    public PageResult<PayTransactionView> pageTransactions(Long projectId, String transactionId, String orderNo,
                                                           String memberName, String payChannel, String orderStatus,
                                                           long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (payChannel != null && !payChannel.isBlank()) {
            validatePayChannel(payChannel);
        }
        validateOrderStatus(orderStatus);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findTransactions(tenantId, scope, projectId, normalize(transactionId),
                        normalize(orderNo), normalize(memberName), normalize(payChannel), normalize(orderStatus),
                        offset(pageNo, pageSize), pageSize),
                repository.countTransactions(tenantId, scope, projectId, normalize(transactionId),
                        normalize(orderNo), normalize(memberName), normalize(payChannel), normalize(orderStatus)),
                pageNo,
                pageSize);
    }

    public PageResult<MemberPrepaymentView> pagePrepayments(Long projectId, String memberName, String orderNo,
                                                            long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findPrepayments(tenantId, scope, projectId, memberName, orderNo, offset(pageNo, pageSize), pageSize),
                repository.countPrepayments(tenantId, scope, projectId, memberName, orderNo),
                pageNo,
                pageSize);
    }

    @Transactional
    public PayOrderCreateResult createOrder(PayOrderCreateRequest request) {
        ensureProjectAllowed(request.projectId());
        validatePayChannel(request.payChannel());
        if ("WECHAT".equals(request.payChannel())) {
            repository.findPaySecret(tenantId(), request.projectId(), "WECHAT");
        }
        PreparedPayment payment = preparePayment(request, "订单金额必须大于0");
        Long orderId = newId();
        String orderNo = "PAY-" + tenantId() + "-" + request.projectId() + "-" + orderId;
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(30);
        repository.insertOrder(tenantId(), orderId, orderNo, userId(), request.projectId(), payment.memberId(),
                request.payChannel(), payment.amount(), "物业缴费 " + orderNo, expireAt);
        for (BillAllocation allocation : payment.allocations()) {
            repository.insertOrderBill(newId(), tenantId(), request.projectId(), orderId,
                    allocation.bill().billId(), allocation.amount());
        }
        return new PayOrderCreateResult(orderId, orderNo, payment.amount(), "PAYING", "LOCAL-" + orderNo, expireAt);
    }

    @Transactional
    public PayOrderCreateResult collectOffline(PayOrderCreateRequest request) {
        ensureProjectAllowed(request.projectId());
        validatePayChannel(request.payChannel());
        if (!OFFLINE_COLLECTION_CHANNELS.contains(request.payChannel())) {
            throw new IllegalArgumentException("线下收款仅支持现金、POS或线下收款");
        }
        PreparedPayment payment = preparePayment(request, "收款金额必须大于0");

        Long orderId = newId();
        String orderNo = "PAY-" + tenantId() + "-" + request.projectId() + "-" + orderId;
        LocalDateTime paidAt = LocalDateTime.now();
        repository.insertOrder(tenantId(), orderId, orderNo, userId(), request.projectId(), payment.memberId(),
                request.payChannel(), payment.amount(), "物业线下收款 " + orderNo, paidAt.plusMinutes(30));
        for (BillAllocation allocation : payment.allocations()) {
            repository.insertOrderBill(newId(), tenantId(), request.projectId(), orderId,
                    allocation.bill().billId(), allocation.amount());
        }

        String thirdTradeNo = request.payChannel() + "-" + orderNo;
        repository.insertTransactionIfAbsent(newId(), tenantId(), request.projectId(), orderId, orderNo,
                thirdTradeNo, request.payChannel(), payment.amount(), paidAt, "{\"source\":\"PC_OFFLINE_COLLECTION\"}");
        settlePaidOrder(tenantId(), request.projectId(), orderId, orderNo, payment.memberId(), request.payChannel(), payment.amount(),
                "现金收款超出应收金额，自动转入预存款");
        repository.markOrderPaid(tenantId(), orderId, thirdTradeNo, paidAt);
        notifyPaymentSuccess(repository.getOrderByNo(orderNo), payment.amount());
        return new PayOrderCreateResult(orderId, orderNo, payment.amount(), "PAID", thirdTradeNo, paidAt);
    }

    @Transactional
    public PaymentNotifyResult handleWechatNotify(WechatPayNotifyRequest request) {
        PayOrderView order = repository.getOrderByNo(request.orderNo());
        if (!"WECHAT".equals(order.payChannel())) {
            throw new IllegalArgumentException("订单支付渠道不是微信：" + request.orderNo());
        }
        if ("PAID".equals(order.status())) {
            return new PaymentNotifyResult(order.orderNo(), "PAID", true);
        }
        verifyWechatSignature(order, request);
        BigDecimal notifyAmount = money(request.amount());
        if (notifyAmount.compareTo(order.amount()) != 0) {
            throw new IllegalArgumentException("回调金额与订单金额不一致");
        }
        if (repository.transactionExists(order.tenantId(), "WECHAT", request.thirdTradeNo())) {
            repository.markOrderPaid(order.tenantId(), order.orderId(), request.thirdTradeNo(), request.paidAt());
            return new PaymentNotifyResult(order.orderNo(), "PAID", true);
        }
        Long tenantId = order.tenantId();
        boolean inserted = repository.insertTransactionIfAbsent(newId(), tenantId, order.projectId(), order.orderId(),
                order.orderNo(), request.thirdTradeNo(), "WECHAT", notifyAmount, request.paidAt(), toJson(request));
        if (!inserted) {
            repository.markOrderPaid(tenantId, order.orderId(), request.thirdTradeNo(), request.paidAt());
            return new PaymentNotifyResult(order.orderNo(), "PAID", true);
        }
        settlePaidOrder(tenantId, order.projectId(), order.orderId(), order.orderNo(), order.memberId(), "WECHAT", notifyAmount,
                "收款码收款超出应收金额，自动转入预存款");
        repository.markOrderPaid(tenantId, order.orderId(), request.thirdTradeNo(), request.paidAt());
        notifyPaymentSuccess(order, notifyAmount);
        return new PaymentNotifyResult(order.orderNo(), "PAID", false);
    }

    @Transactional
    public PaymentNotifyResult confirmDemoAppPayment(String orderNo) {
        PayOrderView order = repository.getOrderByNo(orderNo);
        if (!tenantId().equals(order.tenantId())) {
            throw new AccessDeniedException("无权操作该支付订单");
        }
        if (order.memberId() == null || !order.memberId().equals(userId())) {
            throw new AccessDeniedException("无权操作该支付订单");
        }
        if (!"WECHAT".equals(order.payChannel())) {
            throw new IllegalArgumentException("业主端演示支付仅支持微信支付订单");
        }
        if ("PAID".equals(order.status())) {
            return new PaymentNotifyResult(order.orderNo(), "PAID", true);
        }
        if (!Set.of("PENDING", "PAYING").contains(order.status())) {
            throw new IllegalArgumentException("当前订单状态不可支付：" + order.status());
        }
        LocalDateTime paidAt = LocalDateTime.now();
        String thirdTradeNo = "APP-DEMO-" + order.orderNo();
        if (repository.transactionExists(order.tenantId(), "WECHAT", thirdTradeNo)) {
            repository.markOrderPaid(order.tenantId(), order.orderId(), thirdTradeNo, paidAt);
            return new PaymentNotifyResult(order.orderNo(), "PAID", true);
        }
        boolean inserted = repository.insertTransactionIfAbsent(newId(), order.tenantId(), order.projectId(),
                order.orderId(), order.orderNo(), thirdTradeNo, "WECHAT", order.amount(), paidAt,
                "{\"source\":\"APP_DEMO_PAYMENT\"}");
        if (inserted) {
            settlePaidOrder(order.tenantId(), order.projectId(), order.orderId(), order.orderNo(), order.memberId(), "WECHAT",
                    order.amount(), "业主端模拟支付超出应收金额，自动转入预存款");
            notifyPaymentSuccess(order, order.amount());
        }
        repository.markOrderPaid(order.tenantId(), order.orderId(), thirdTradeNo, paidAt);
        return new PaymentNotifyResult(order.orderNo(), "PAID", !inserted);
    }

    private void settlePaidOrder(Long tenantId, Long projectId, Long orderId, String orderNo, Long memberId, String payChannel,
                                 BigDecimal paidAmount, String prepaymentRemark) {
        BigDecimal unappliedAmount = money(paidAmount);
        BigDecimal appliedAmount = BigDecimal.ZERO;
        for (OrderBillSettlement bill : repository.findOrderBillSettlements(tenantId, orderId)) {
            if (unappliedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal allocated = money(bill.allocatedAmount());
            BigDecimal currentRemaining = money(bill.currentRemainingAmount());
            BigDecimal amount = allocated.min(currentRemaining).min(unappliedAmount).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int updated = repository.settleBill(tenantId, bill.billId(), amount);
            if (updated == 0) {
                continue;
            }
            appliedAmount = appliedAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
            unappliedAmount = unappliedAmount.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal prepaymentAmount = paidAmount.subtract(appliedAmount).setScale(2, RoundingMode.HALF_UP);
        if (prepaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            repository.insertPrepayment(newId(), tenantId, projectId, memberId, orderId, orderNo,
                    prepaymentAmount, prepaymentSource(payChannel), userId(), prepaymentRemark);
        }
    }

    private String prepaymentSource(String payChannel) {
        return switch (payChannel) {
            case "WECHAT" -> "WECHAT_OVERPAY";
            case "POS" -> "POS_OVERPAY";
            case "BANK_TRANSFER" -> "BANK_TRANSFER_OVERPAY";
            default -> "OFFLINE_OVERPAY";
        };
    }

    private Long validatePayableBills(List<PayableBill> bills) {
        Long memberId = null;
        for (PayableBill bill : bills) {
            if (!PAYABLE_BILL_STATUSES.contains(bill.status())) {
                throw new IllegalArgumentException("账单状态不可支付：" + bill.billNo());
            }
            if (memberId == null) {
                memberId = bill.memberId();
            } else if (bill.memberId() != null && !memberId.equals(bill.memberId())) {
                throw new IllegalArgumentException("合并支付账单必须属于同一会员");
            }
        }
        return memberId;
    }

    private void verifyWechatSignature(PayOrderView order, WechatPayNotifyRequest request) {
        Long tenantId = order.tenantId();
        String secret = repository.findPaySecret(tenantId, order.projectId(), "WECHAT");
        wechatPayClient.verifyPayNotify(order, secret, request);
    }

    private List<Long> distinctBillIds(List<Long> billIds) {
        return new LinkedHashSet<>(billIds).stream().toList();
    }

    private void validatePayChannel(String payChannel) {
        if (!PAY_CHANNELS.contains(payChannel)) {
            throw new IllegalArgumentException("非法支付渠道：" + payChannel);
        }
    }

    private PreparedPayment preparePayment(PayOrderCreateRequest request, String amountErrorMessage) {
        List<Long> billIds = distinctBillIds(request.billIds());
        List<PayableBill> bills = repository.findPayableBills(tenantId(), request.projectId(), billIds);
        if (bills.size() != billIds.size()) {
            throw new IllegalArgumentException("账单不存在或不属于项目");
        }
        Long memberId = validatePayableBills(bills);
        BigDecimal dueAmount = bills.stream()
                .map(PayableBill::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("账单待收金额必须大于0");
        }
        BigDecimal amount = request.amount() == null ? dueAmount : money(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(amountErrorMessage);
        }
        BigDecimal appliedAmount = amount.min(dueAmount);
        List<BillAllocation> allocations = allocateBills(bills, appliedAmount);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("本次收款没有可核销账单");
        }
        BigDecimal prepaymentAmount = amount.subtract(appliedAmount).setScale(2, RoundingMode.HALF_UP);
        return new PreparedPayment(allocations, memberId, dueAmount, amount, prepaymentAmount);
    }

    private List<BillAllocation> allocateBills(List<PayableBill> bills, BigDecimal amount) {
        BigDecimal remaining = amount;
        java.util.ArrayList<BillAllocation> allocations = new java.util.ArrayList<>();
        for (PayableBill bill : bills) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal billAmount = money(bill.remainingAmount());
            if (billAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocated = billAmount.min(remaining).setScale(2, RoundingMode.HALF_UP);
            allocations.add(new BillAllocation(bill, allocated));
            remaining = remaining.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
        }
        return allocations;
    }

    private void validateOrderStatus(String status) {
        if (status != null && !status.isBlank() && !ORDER_STATUSES.contains(status)) {
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
            throw new IllegalArgumentException("支付回调报文无法序列化", ex);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void notifyPaymentSuccess(PayOrderView order, BigDecimal paidAmount) {
        if (order.memberId() == null) {
            return;
        }
        String content = "您的物业缴费已入账。"
                + "\n订单号：" + order.orderNo()
                + "\n本次收款：" + money(paidAmount) + " 元"
                + "\n支付方式：" + payChannelText(order.payChannel())
                + (order.billSummary() == null || order.billSummary().isBlank()
                ? "" : "\n账单明细：" + order.billSummary());
        appMessageService.sendToMember(order.tenantId(), order.projectId(), order.memberId(), "PAYMENT_SUCCESS",
                "缴费成功", content, Map.of(
                        "orderNo", order.orderNo(),
                        "memberName", text(order.memberName(), ""),
                        "houseNo", text(order.houseNo(), ""),
                        "paidAmount", money(paidAmount),
                        "payChannel", payChannelText(order.payChannel()),
                        "billSummary", text(order.billSummary(), "")
                ));
    }

    private String payChannelText(String payChannel) {
        return switch (payChannel == null ? "" : payChannel) {
            case "WECHAT" -> "微信";
            case "ALI" -> "支付宝";
            case "OFFLINE" -> "线下收款";
            case "POS" -> "POS";
            case "CASH" -> "现金";
            case "BANK_TRANSFER" -> "银行转账";
            default -> payChannel == null ? "-" : payChannel;
        };
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
