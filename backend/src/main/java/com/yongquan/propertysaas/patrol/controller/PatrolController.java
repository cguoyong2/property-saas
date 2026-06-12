package com.yongquan.propertysaas.patrol.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.patrol.domain.AssetEquipmentView;
import com.yongquan.propertysaas.patrol.domain.PatrolPlanView;
import com.yongquan.propertysaas.patrol.domain.PatrolPointView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskDetailView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskView;
import com.yongquan.propertysaas.patrol.dto.AssetEquipmentRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPlanRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPointRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolTaskCreateRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolTaskSubmitRequest;
import com.yongquan.propertysaas.patrol.dto.RectificationRequest;
import com.yongquan.propertysaas.patrol.service.PatrolService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
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
public class PatrolController {

    private final PatrolService service;

    public PatrolController(PatrolService service) {
        this.service = service;
    }

    @GetMapping("/api/patrol/assets")
    @RequiresPermission("patrol:asset:list")
    public ApiResponse<PageResult<AssetEquipmentView>> assets(@RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageEquipments(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/patrol/assets")
    @RequiresPermission("patrol:asset:create")
    public ApiResponse<Map<String, Long>> createAsset(@Valid @RequestBody AssetEquipmentRequest request) {
        return ApiResponse.success(Map.of("equipmentId", service.createEquipment(request)));
    }

    @GetMapping("/api/patrol/points")
    @RequiresPermission("patrol:point:list")
    public ApiResponse<PageResult<PatrolPointView>> points(@RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") long pageNo,
                                                           @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePoints(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/patrol/points")
    @RequiresPermission("patrol:point:create")
    public ApiResponse<Map<String, Long>> createPoint(@Valid @RequestBody PatrolPointRequest request) {
        return ApiResponse.success(Map.of("pointId", service.createPoint(request)));
    }

    @GetMapping("/api/patrol/plans")
    @RequiresPermission("patrol:plan:list")
    public ApiResponse<PageResult<PatrolPlanView>> plans(@RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") long pageNo,
                                                         @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePlans(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/patrol/plans")
    @RequiresPermission("patrol:plan:create")
    public ApiResponse<Map<String, Long>> createPlan(@Valid @RequestBody PatrolPlanRequest request) {
        return ApiResponse.success(Map.of("planId", service.createPlan(request)));
    }

    @GetMapping("/api/patrol/tasks")
    @RequiresPermission("patrol:task:list")
    public ApiResponse<PageResult<PatrolTaskView>> tasks(@RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long executorUserId,
                                                         @RequestParam(defaultValue = "1") long pageNo,
                                                         @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageTasks(projectId, status, executorUserId, pageNo, pageSize));
    }

    @PostMapping("/api/patrol/tasks")
    @RequiresPermission("patrol:task:create")
    public ApiResponse<Map<String, Long>> createTask(@Valid @RequestBody PatrolTaskCreateRequest request) {
        return ApiResponse.success(Map.of("taskId", service.createTask(request)));
    }

    @GetMapping("/api/patrol/tasks/{taskId}")
    @RequiresPermission("patrol:task:view")
    public ApiResponse<PatrolTaskDetailView> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(service.getTask(taskId));
    }

    @PostMapping("/api/patrol/tasks/{taskId}/submit")
    @RequiresPermission("patrol:task:submit")
    public ApiResponse<PatrolTaskDetailView> submitTask(@PathVariable Long taskId,
                                                        @Valid @RequestBody PatrolTaskSubmitRequest request) {
        return ApiResponse.success(service.submitTask(taskId, request));
    }

    @PutMapping("/api/patrol/tasks/{taskId}/items/{itemId}/rectify")
    @RequiresPermission("patrol:task:rectify")
    public ApiResponse<Void> startRectification(@PathVariable Long taskId,
                                                @PathVariable Long itemId,
                                                @RequestBody(required = false) RectificationRequest request) {
        service.startRectification(taskId, itemId, request == null ? new RectificationRequest(null, null) : request);
        return ApiResponse.success();
    }

    @PutMapping("/api/patrol/tasks/{taskId}/items/{itemId}/rectify-complete")
    @RequiresPermission("patrol:task:rectify")
    public ApiResponse<PatrolTaskDetailView> completeRectification(@PathVariable Long taskId,
                                                                   @PathVariable Long itemId,
                                                                   @RequestBody(required = false) RectificationRequest request) {
        return ApiResponse.success(service.completeRectification(taskId, itemId,
                request == null ? new RectificationRequest(null, null) : request));
    }

    @PostMapping("/api/patrol/tasks/mark-missed")
    @RequiresPermission("patrol:task:missed")
    public ApiResponse<Map<String, Integer>> markMissed(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(Map.of("missedCount", service.markMissed(limit)));
    }
}
