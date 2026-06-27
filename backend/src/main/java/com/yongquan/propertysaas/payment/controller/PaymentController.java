package com.yongquan.propertysaas.payment.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.MemberPrepaymentView;
import com.yongquan.propertysaas.payment.domain.PayOrderCreateResult;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayTransactionView;
import com.yongquan.propertysaas.payment.domain.PaymentNotifyResult;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionView;
import com.yongquan.propertysaas.payment.domain.ReconcileSummaryView;
import com.yongquan.propertysaas.payment.domain.RefundableOrderView;
import com.yongquan.propertysaas.payment.domain.RefundNotifyResult;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.payment.dto.ReconcileExceptionHandleRequest;
import com.yongquan.propertysaas.payment.dto.RefundAuditRequest;
import com.yongquan.propertysaas.payment.dto.RefundCreateRequest;
import com.yongquan.propertysaas.payment.dto.WechatRefundNotifyRequest;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.service.PaymentRefundService;
import com.yongquan.propertysaas.payment.service.PaymentService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService service;
    private final PaymentRefundService refundService;

    public PaymentController(PaymentService service, PaymentRefundService refundService) {
        this.service = service;
        this.refundService = refundService;
    }

    @PostMapping("/api/payment/orders")
    @RequiresPermission("payment:order:create")
    public ApiResponse<PayOrderCreateResult> createOrder(@Valid @RequestBody PayOrderCreateRequest request) {
        return ApiResponse.success(service.createOrder(request));
    }

    @PostMapping("/api/payment/offline-collections")
    @RequiresPermission("payment:order:create")
    public ApiResponse<PayOrderCreateResult> collectOffline(@Valid @RequestBody PayOrderCreateRequest request) {
        return ApiResponse.success(service.collectOffline(request));
    }

    @GetMapping("/api/payment/orders")
    @RequiresPermission("payment:order:list")
    public ApiResponse<PageResult<PayOrderView>> pageOrders(@RequestParam(required = false) Long projectId,
                                                            @RequestParam(required = false) String orderNo,
                                                            @RequestParam(required = false) String memberName,
                                                            @RequestParam(required = false) String payChannel,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") long pageNo,
                                                            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageOrders(projectId, orderNo, memberName, payChannel, status, pageNo, pageSize));
    }

    @GetMapping("/api/payment/orders.csv")
    @RequiresPermission("payment:order:list")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) Long projectId,
                                               @RequestParam(required = false) String orderNo,
                                               @RequestParam(required = false) String memberName,
                                               @RequestParam(required = false) String payChannel,
                                               @RequestParam(required = false) String status) {
        List<PayOrderView> rows = exportPages(pageNo -> service.pageOrders(projectId, orderNo, memberName,
                payChannel, status, pageNo, 200));
        StringBuilder csv = csvHeader("订单号", "小区ID", "业主/住户", "手机号", "房号", "应收明细", "订单金额",
                "实收金额", "已退金额", "可退金额", "支付方式", "状态", "支付时间", "创建时间");
        for (PayOrderView row : rows) {
            csvRow(csv, row.orderNo(), row.projectId(), row.memberName(), row.memberMobile(), row.houseNo(),
                    row.billSummary(), row.amount(), row.transactionAmount(), row.refundedAmount(), row.refundableAmount(),
                    payChannelLabel(row.payChannel()), orderStatusLabel(row.status()), row.paidAt(), row.createdAt());
        }
        return csv("payment-orders.csv", csv);
    }

    @GetMapping("/api/payment/transactions")
    @RequiresPermission("payment:transaction:list")
    public ApiResponse<PageResult<PayTransactionView>> pageTransactions(@RequestParam(required = false) Long projectId,
                                                                        @RequestParam(required = false) String transactionId,
                                                                        @RequestParam(required = false) String orderNo,
                                                                        @RequestParam(required = false) String memberName,
                                                                        @RequestParam(required = false) String payChannel,
                                                                        @RequestParam(required = false) String orderStatus,
                                                                        @RequestParam(defaultValue = "1") long pageNo,
                                                                        @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageTransactions(projectId, transactionId, orderNo, memberName,
                payChannel, orderStatus, pageNo, pageSize));
    }

    @GetMapping("/api/payment/transactions.csv")
    @RequiresPermission("payment:transaction:list")
    public ResponseEntity<byte[]> exportTransactions(@RequestParam(required = false) Long projectId,
                                                     @RequestParam(required = false) String transactionId,
                                                     @RequestParam(required = false) String orderNo,
                                                     @RequestParam(required = false) String memberName,
                                                     @RequestParam(required = false) String payChannel,
                                                     @RequestParam(required = false) String orderStatus) {
        List<PayTransactionView> rows = exportPages(pageNo -> service.pageTransactions(projectId, transactionId, orderNo,
                memberName, payChannel, orderStatus, pageNo, 200));
        StringBuilder csv = csvHeader("流水ID", "小区ID", "订单号", "业主/住户", "手机号", "房号", "应收明细",
                "第三方流水号", "支付方式", "流水金额", "订单金额", "核销账单金额", "转预存款", "订单状态", "支付时间");
        for (PayTransactionView row : rows) {
            csvRow(csv, row.transactionId(), row.projectId(), row.orderNo(), row.memberName(), row.memberMobile(),
                    row.houseNo(), row.billSummary(), row.thirdTradeNo(), payChannelLabel(row.payChannel()), row.amount(),
                    row.orderAmount(), row.billAppliedAmount(), row.prepaymentAmount(), orderStatusLabel(row.orderStatus()), row.paidAt());
        }
        return csv("payment-transactions.csv", csv);
    }

    @GetMapping("/api/payment/prepayments")
    @RequiresPermission("payment:prepayment:list")
    public ApiResponse<PageResult<MemberPrepaymentView>> pagePrepayments(@RequestParam(required = false) Long projectId,
                                                                         @RequestParam(required = false) String memberName,
                                                                         @RequestParam(required = false) String orderNo,
                                                                         @RequestParam(defaultValue = "1") long pageNo,
                                                                         @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePrepayments(projectId, memberName, orderNo, pageNo, pageSize));
    }

    @PostMapping("/api/payment/wechat/notify")
    public ApiResponse<PaymentNotifyResult> wechatNotify(@Valid @RequestBody WechatPayNotifyRequest request) {
        return ApiResponse.success(service.handleWechatNotify(request));
    }

    @GetMapping("/api/payment/refunds")
    @RequiresPermission("payment:refund:list")
    public ApiResponse<PageResult<PayRefundView>> pageRefunds(@RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) String refundNo,
                                                              @RequestParam(required = false) String orderNo,
                                                              @RequestParam(required = false) String memberName,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(refundService.pageRefunds(projectId, refundNo, orderNo, memberName, status, pageNo, pageSize));
    }

    @GetMapping("/api/payment/refunds.csv")
    @RequiresPermission("payment:refund:list")
    public ResponseEntity<byte[]> exportRefunds(@RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) String refundNo,
                                                @RequestParam(required = false) String orderNo,
                                                @RequestParam(required = false) String memberName,
                                                @RequestParam(required = false) String status) {
        List<PayRefundView> rows = exportPages(pageNo -> refundService.pageRefunds(projectId, refundNo, orderNo,
                memberName, status, pageNo, 200));
        StringBuilder csv = csvHeader("退款单号", "小区ID", "原订单号", "业主/住户", "手机号", "房号", "原账单明细", "原支付方式",
                "原订单金额", "申请退款金额", "已退流水", "状态", "原因", "退款时间", "创建时间");
        for (PayRefundView row : rows) {
            csvRow(csv, row.refundNo(), row.projectId(), row.orderNo(), row.memberName(), row.memberMobile(),
                    row.houseNo(), row.billSummary(), payChannelLabel(row.payChannel()), row.orderAmount(), row.refundAmount(),
                    row.refundedTransactionAmount(), refundStatusLabel(row.status()), row.reason(), row.refundedAt(), row.createdAt());
        }
        return csv("payment-refunds.csv", csv);
    }

    @GetMapping("/api/payment/refundable-orders")
    @RequiresPermission("payment:refund:create")
    public ApiResponse<List<RefundableOrderView>> refundableOrders(@RequestParam Long projectId,
                                                                   @RequestParam Long memberId) {
        return ApiResponse.success(refundService.refundableOrders(projectId, memberId));
    }

    @PostMapping("/api/payment/refunds")
    @RequiresPermission("payment:refund:create")
    public ApiResponse<Map<String, Long>> createRefund(@Valid @RequestBody RefundCreateRequest request) {
        return ApiResponse.success(Map.of("refundId", refundService.createRefund(request)));
    }

    @PostMapping("/api/payment/refunds/{refundId}/audit")
    @RequiresPermission("payment:refund:audit")
    public ApiResponse<Void> auditRefund(@PathVariable Long refundId, @Valid @RequestBody RefundAuditRequest request) {
        refundService.auditRefund(refundId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/payment/refunds/{refundId}/offline-confirm")
    @RequiresPermission("payment:refund:audit")
    public ApiResponse<Void> confirmOfflineRefund(@PathVariable Long refundId, @Valid @RequestBody RefundAuditRequest request) {
        refundService.confirmOfflineRefund(refundId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/payment/wechat/refund-notify")
    public ApiResponse<RefundNotifyResult> wechatRefundNotify(@Valid @RequestBody WechatRefundNotifyRequest request) {
        return ApiResponse.success(refundService.handleWechatRefundNotify(request));
    }

    @GetMapping("/api/payment/reconcile")
    @RequiresPermission("payment:reconcile:view")
    public ApiResponse<ReconcileSummaryView> reconcile(@RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate,
                                                       @RequestParam(required = false) String payChannel,
                                                       @RequestParam(required = false) String orderStatus) {
        return ApiResponse.success(refundService.reconcile(projectId, startDate, endDate, payChannel, orderStatus));
    }

    @GetMapping("/api/payment/reconcile/exceptions")
    @RequiresPermission("payment:reconcile:view")
    public ApiResponse<PageResult<ReconcileExceptionView>> reconcileExceptions(@RequestParam(required = false) Long projectId,
                                                                               @RequestParam(required = false) String exceptionType,
                                                                               @RequestParam(required = false) String businessNo,
                                                                               @RequestParam(required = false) String memberName,
                                                                               @RequestParam(required = false) String status,
                                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(refundService.pageReconcileExceptions(projectId, exceptionType, businessNo,
                memberName, status, pageNo, pageSize));
    }

    @PostMapping("/api/payment/reconcile/exceptions/{exceptionKey}/handle")
    @RequiresPermission("payment:reconcile:view")
    public ApiResponse<Void> handleReconcileException(@PathVariable String exceptionKey,
                                                      @Valid @RequestBody ReconcileExceptionHandleRequest request) {
        refundService.handleReconcileException(exceptionKey, request);
        return ApiResponse.success();
    }

    private ResponseEntity<byte[]> csv(String filename, StringBuilder csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8));
    }

    private <T> List<T> exportPages(PageFetcher<T> fetcher) {
        List<T> rows = new ArrayList<>();
        for (long pageNo = 1; pageNo <= 50; pageNo++) {
            PageResult<T> page = fetcher.fetch(pageNo);
            if (page.records().isEmpty()) {
                break;
            }
            rows.addAll(page.records());
            if (rows.size() >= page.total()) {
                break;
            }
        }
        return rows;
    }

    @FunctionalInterface
    private interface PageFetcher<T> {
        PageResult<T> fetch(long pageNo);
    }

    private StringBuilder csvHeader(String... values) {
        StringBuilder csv = new StringBuilder();
        csvRow(csv, (Object[]) values);
        return csv;
    }

    private void csvRow(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvCell(values[i]));
        }
        csv.append('\n');
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    private String payChannelLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case "WECHAT" -> "微信支付";
            case "ALI" -> "支付宝";
            case "CASH" -> "现金";
            case "POS" -> "POS刷卡";
            case "BANK_TRANSFER" -> "银行转账";
            case "OFFLINE" -> "线下收款";
            default -> value;
        };
    }

    private String orderStatusLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case "PENDING" -> "待支付";
            case "PAYING" -> "支付中";
            case "PAID" -> "已支付";
            case "CLOSED" -> "已关闭";
            case "FAILED" -> "支付失败";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            case "PARTIAL_REFUNDED" -> "部分退款";
            default -> value;
        };
    }

    private String refundStatusLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case "APPLYING" -> "待审核";
            case "AUDIT_PASSED" -> "审核通过";
            case "AUDIT_REJECTED" -> "审核驳回";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            case "CLOSED" -> "已关闭";
            case "FAILED" -> "退款失败";
            default -> value;
        };
    }
}
