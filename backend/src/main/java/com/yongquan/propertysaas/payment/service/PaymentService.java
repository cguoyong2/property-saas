package com.yongquan.propertysaas.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.MemberPrepaymentView;
import com.yongquan.propertysaas.payment.domain.PayOrderCreateResult;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayTransactionView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.PaymentNotifyResult;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.repository.PaymentRepository;
import com.yongquan.propertysaas.payment.wechat.WechatPayClient;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Set<String> PAY_CHANNELS = Set.of("WECHAT", "ALI", "OFFLINE", "POS", "CASH");
    private static final Set<String> OFFLINE_COLLECTION_CHANNELS = Set.of("OFFLINE", "POS", "CASH");
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING", "PAYING", "PAID", "CLOSED", "FAILED", "REFUNDING", "REFUNDED", "PARTIAL_REFUNDED");
    private static final Set<String> PAYABLE_BILL_STATUSES = Set.of("UNPAID", "OVERDUE", "PARTIAL_PAID");

    private final PaymentRepository repository;
    private final WechatPayClient wechatPayClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    private record BillAllocation(PayableBill bill, BigDecimal amount) {
    }

    private record PreparedPayment(List<BillAllocation> allocations, Long memberId, BigDecimal dueAmount,
                                   BigDecimal amount, BigDecimal prepaymentAmount) {
    }

    public PaymentService(PaymentRepository repository, WechatPayClient wechatPayClient, ObjectMapper objectMapper) {
        this.repository = repository;
        this.wechatPayClient = wechatPayClient;
        this.objectMapper = objectMapper;
    }

    public PageResult<PayOrderView> pageOrders(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateOrderStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findOrders(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countOrders(tenantId, scope, projectId, status),
                pageNo,
                pageSize);
    }

    public PageResult<PayTransactionView> pageTransactions(Long projectId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findTransactions(tenantId, scope, projectId, offset(pageNo, pageSize), pageSize),
                repository.countTransactions(tenantId, scope, projectId),
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
        List<Long> allocatedBillIds = payment.allocations().stream().map(allocation -> allocation.bill().billId()).toList();
        int marked = repository.markBillsPaying(tenantId(), request.projectId(), allocatedBillIds);
        if (marked != allocatedBillIds.size()) {
            throw new IllegalArgumentException("账单状态已变化，创建支付订单失败");
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
        List<Long> allocatedBillIds = payment.allocations().stream().map(allocation -> allocation.bill().billId()).toList();
        int marked = repository.markBillsPaying(tenantId(), request.projectId(), allocatedBillIds);
        if (marked != allocatedBillIds.size()) {
            throw new IllegalArgumentException("账单状态已变化，收款失败");
        }

        String thirdTradeNo = request.payChannel() + "-" + orderNo;
        repository.insertTransactionIfAbsent(newId(), tenantId(), request.projectId(), orderId, orderNo,
                thirdTradeNo, request.payChannel(), payment.amount(), paidAt, "{\"source\":\"PC_OFFLINE_COLLECTION\"}");
        for (PayableBill bill : repository.findOrderBills(tenantId(), orderId)) {
            repository.settleBill(tenantId(), bill.billId(), bill.remainingAmount());
        }
        if (payment.prepaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            repository.insertPrepayment(newId(), tenantId(), request.projectId(), payment.memberId(), orderId, orderNo,
                    payment.prepaymentAmount(), userId(), "现金收款超出应收金额，自动转入预存款");
        }
        repository.markOrderPaid(tenantId(), orderId, thirdTradeNo, paidAt);
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
        BigDecimal appliedAmount = BigDecimal.ZERO;
        List<PayableBill> orderBills = repository.findOrderBills(tenantId, order.orderId());
        for (PayableBill bill : orderBills) {
            repository.settleBill(tenantId, bill.billId(), bill.remainingAmount());
            appliedAmount = appliedAmount.add(bill.remainingAmount());
        }
        BigDecimal prepaymentAmount = notifyAmount.subtract(appliedAmount).setScale(2, RoundingMode.HALF_UP);
        if (prepaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            repository.insertPrepayment(newId(), tenantId, order.projectId(), order.memberId(), order.orderId(),
                    order.orderNo(), prepaymentAmount, null, "收款码收款超出应收金额，自动转入预存款");
        }
        repository.markOrderPaid(tenantId, order.orderId(), request.thirdTradeNo(), request.paidAt());
        return new PaymentNotifyResult(order.orderNo(), "PAID", false);
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

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
