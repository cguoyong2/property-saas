package com.yongquan.propertysaas.service.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.service.domain.WorkOrderDetailView;
import com.yongquan.propertysaas.service.domain.WorkOrderView;
import com.yongquan.propertysaas.service.dto.WorkOrderActionRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderCreateRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderDispatchRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderEvaluateRequest;
import com.yongquan.propertysaas.service.service.WorkOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @GetMapping("/api/service/workorders")
    @RequiresPermission("service:workorder:list")
    public ApiResponse<PageResult<WorkOrderView>> pageWorkOrders(@RequestParam(required = false) Long projectId,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String orderType,
                                                                 @RequestParam(required = false) Long handlerUserId,
                                                                 @RequestParam(required = false) Long memberId,
                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageWorkOrders(projectId, status, orderType, handlerUserId, memberId, pageNo, pageSize));
    }

    @GetMapping("/api/service/complaints")
    @RequiresPermission("service:complaint:list")
    public ApiResponse<PageResult<WorkOrderView>> pageComplaints(@RequestParam(required = false) Long projectId,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageComplaints(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/service/workorders")
    @RequiresPermission("service:workorder:create")
    public ApiResponse<Map<String, Long>> createWorkOrder(@Valid @RequestBody WorkOrderCreateRequest request) {
        return ApiResponse.success(Map.of("workOrderId", service.createWorkOrder(request)));
    }

    @GetMapping("/api/service/workorders/{workOrderId}")
    @RequiresPermission("service:workorder:view")
    public ApiResponse<WorkOrderDetailView> getWorkOrder(@PathVariable Long workOrderId) {
        return ApiResponse.success(service.getWorkOrder(workOrderId));
    }

    @PutMapping("/api/service/workorders/{workOrderId}/accept")
    @RequiresPermission("service:workorder:accept")
    public ApiResponse<Void> accept(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.accept(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/reject")
    @RequiresPermission("service:workorder:reject")
    public ApiResponse<Void> reject(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.reject(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/dispatch")
    @RequiresPermission("service:workorder:dispatch")
    public ApiResponse<Void> dispatch(@PathVariable Long workOrderId,
                                      @Valid @RequestBody WorkOrderDispatchRequest request) {
        service.dispatch(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/start")
    @RequiresPermission("service:workorder:process")
    public ApiResponse<Void> startProcessing(@PathVariable Long workOrderId,
                                             @RequestBody(required = false) WorkOrderActionRequest request) {
        service.startProcessing(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/hang-up")
    @RequiresPermission("service:workorder:process")
    public ApiResponse<Void> hangUp(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.hangUp(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/resume")
    @RequiresPermission("service:workorder:process")
    public ApiResponse<Void> resume(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.resume(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/submit-result")
    @RequiresPermission("service:workorder:process")
    public ApiResponse<Void> submitResult(@PathVariable Long workOrderId,
                                          @RequestBody(required = false) WorkOrderActionRequest request) {
        service.submitResult(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/rework")
    @RequiresPermission("service:workorder:revisit")
    public ApiResponse<Void> rework(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.rework(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/revisit")
    @RequiresPermission("service:workorder:revisit")
    public ApiResponse<Void> revisit(@PathVariable Long workOrderId,
                                     @RequestBody(required = false) WorkOrderActionRequest request) {
        service.revisit(workOrderId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/workorders/{workOrderId}/cancel")
    @RequiresPermission("service:workorder:cancel")
    public ApiResponse<Void> cancel(@PathVariable Long workOrderId,
                                    @RequestBody(required = false) WorkOrderActionRequest request) {
        service.cancel(workOrderId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/service/workorders/sla/mark-overdue")
    @RequiresPermission("service:workorder:sla")
    public ApiResponse<Map<String, Integer>> markSlaOverdue(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(Map.of("overdueCount", service.markSlaOverdue(limit)));
    }

    @PostMapping("/api/app/workorders")
    @RequiresPermission("app:workorder:create")
    public ApiResponse<Map<String, Long>> appCreateWorkOrder(@Valid @RequestBody WorkOrderCreateRequest request) {
        return ApiResponse.success(Map.of("workOrderId", service.createWorkOrder(request)));
    }

    @GetMapping("/api/app/workorders")
    @RequiresPermission("app:workorder:list")
    public ApiResponse<PageResult<WorkOrderView>> appWorkOrders(@RequestParam Long projectId,
                                                                @RequestParam Long memberId,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageWorkOrders(projectId, null, null, null, memberId, pageNo, pageSize));
    }

    @GetMapping("/api/app/workorders/{workOrderId}")
    @RequiresPermission("app:workorder:view")
    public ApiResponse<WorkOrderDetailView> appWorkOrder(@PathVariable Long workOrderId) {
        return ApiResponse.success(service.getWorkOrder(workOrderId));
    }

    @PostMapping("/api/app/workorders/{workOrderId}/evaluate")
    @RequiresPermission("app:workorder:evaluate")
    public ApiResponse<Void> appEvaluate(@PathVariable Long workOrderId,
                                         @Valid @RequestBody WorkOrderEvaluateRequest request) {
        service.evaluate(workOrderId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/app/complaints")
    @RequiresPermission("app:complaint:create")
    public ApiResponse<Map<String, Long>> appCreateComplaint(@Valid @RequestBody WorkOrderCreateRequest request) {
        WorkOrderCreateRequest complaint = new WorkOrderCreateRequest(request.projectId(), request.memberId(),
                request.houseId(), "COMPLAINT", request.title(), request.description(), request.location(),
                request.imageFileIds(), request.priority(), request.handlerUserId());
        return ApiResponse.success(Map.of("workOrderId", service.createWorkOrder(complaint)));
    }
}
