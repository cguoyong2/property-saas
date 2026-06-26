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
import java.util.List;
import java.util.Map;
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
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") long pageNo,
                                                            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageOrders(projectId, orderNo, memberName, status, pageNo, pageSize));
    }

    @GetMapping("/api/payment/transactions")
    @RequiresPermission("payment:transaction:list")
    public ApiResponse<PageResult<PayTransactionView>> pageTransactions(@RequestParam(required = false) Long projectId,
                                                                        @RequestParam(required = false) String transactionId,
                                                                        @RequestParam(required = false) String orderNo,
                                                                        @RequestParam(required = false) String memberName,
                                                                        @RequestParam(defaultValue = "1") long pageNo,
                                                                        @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageTransactions(projectId, transactionId, orderNo, memberName, pageNo, pageSize));
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
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(refundService.pageRefunds(projectId, status, pageNo, pageSize));
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
                                                                               @RequestParam(required = false) String status,
                                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(refundService.pageReconcileExceptions(projectId, exceptionType, status, pageNo, pageSize));
    }

    @PostMapping("/api/payment/reconcile/exceptions/{exceptionKey}/handle")
    @RequiresPermission("payment:reconcile:view")
    public ApiResponse<Void> handleReconcileException(@PathVariable String exceptionKey,
                                                      @Valid @RequestBody ReconcileExceptionHandleRequest request) {
        refundService.handleReconcileException(exceptionKey, request);
        return ApiResponse.success();
    }
}
