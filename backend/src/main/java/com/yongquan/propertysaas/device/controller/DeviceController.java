package com.yongquan.propertysaas.device.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.device.domain.AccessPermissionView;
import com.yongquan.propertysaas.device.domain.AccessRecordView;
import com.yongquan.propertysaas.device.domain.DeviceConfigView;
import com.yongquan.propertysaas.device.domain.DeviceSyncResultView;
import com.yongquan.propertysaas.device.domain.VisitorRecordView;
import com.yongquan.propertysaas.device.dto.AccessPermissionRequest;
import com.yongquan.propertysaas.device.dto.AccessRecordRequest;
import com.yongquan.propertysaas.device.dto.AccessSyncRequest;
import com.yongquan.propertysaas.device.dto.DeviceConfigRequest;
import com.yongquan.propertysaas.device.dto.VisitorInviteRequest;
import com.yongquan.propertysaas.device.service.DeviceService;
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
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping("/api/device/configs")
    @RequiresPermission("device:config:list")
    public ApiResponse<PageResult<DeviceConfigView>> pageDevices(@RequestParam(required = false) Long projectId,
                                                                 @RequestParam(required = false) String deviceType,
                                                                 @RequestParam(required = false) String vendorCode,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageDevices(projectId, deviceType, vendorCode, status, pageNo, pageSize));
    }

    @PostMapping("/api/device/configs")
    @RequiresPermission("device:config:create")
    public ApiResponse<Map<String, Long>> createDevice(@Valid @RequestBody DeviceConfigRequest request) {
        return ApiResponse.success(Map.of("deviceId", service.createDevice(request)));
    }

    @GetMapping("/api/device/configs/{deviceId}")
    @RequiresPermission("device:config:view")
    public ApiResponse<DeviceConfigView> getDevice(@PathVariable Long deviceId) {
        return ApiResponse.success(service.getDevice(deviceId));
    }

    @PutMapping("/api/device/configs/{deviceId}")
    @RequiresPermission("device:config:update")
    public ApiResponse<Void> updateDevice(@PathVariable Long deviceId, @Valid @RequestBody DeviceConfigRequest request) {
        service.updateDevice(deviceId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/device/visitors")
    @RequiresPermission("device:visitor:list")
    public ApiResponse<PageResult<VisitorRecordView>> pageVisitors(@RequestParam(required = false) Long projectId,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(defaultValue = "1") long pageNo,
                                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageVisitors(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/device/visitors")
    @RequiresPermission("device:visitor:create")
    public ApiResponse<Map<String, Long>> createVisitor(@Valid @RequestBody VisitorInviteRequest request) {
        return ApiResponse.success(Map.of("visitorId", service.createVisitorInvite(request)));
    }

    @GetMapping("/api/device/access")
    @RequiresPermission("device:access:list")
    public ApiResponse<PageResult<AccessPermissionView>> pageAccessPermissions(@RequestParam(required = false) Long projectId,
                                                                               @RequestParam(required = false) Long deviceId,
                                                                               @RequestParam(required = false) String status,
                                                                               @RequestParam(required = false) String syncStatus,
                                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageAccessPermissions(projectId, deviceId, status, syncStatus, pageNo, pageSize));
    }

    @PostMapping("/api/device/access")
    @RequiresPermission("device:access:create")
    public ApiResponse<Map<String, Long>> createAccessPermission(@Valid @RequestBody AccessPermissionRequest request) {
        return ApiResponse.success(Map.of("permissionId", service.createAccessPermission(request)));
    }

    @PostMapping("/api/device/access/sync")
    @RequiresPermission("device:access:sync")
    public ApiResponse<DeviceSyncResultView> syncAccessPermissions(@RequestBody(required = false) AccessSyncRequest request) {
        return ApiResponse.success(service.syncAccessPermissions(request));
    }

    @GetMapping("/api/device/access/records")
    @RequiresPermission("device:access:record:list")
    public ApiResponse<PageResult<AccessRecordView>> pageAccessRecords(@RequestParam(required = false) Long projectId,
                                                                       @RequestParam(required = false) Long deviceId,
                                                                       @RequestParam(defaultValue = "1") long pageNo,
                                                                       @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageAccessRecords(projectId, deviceId, pageNo, pageSize));
    }

    @PostMapping("/api/device/access/records")
    @RequiresPermission("device:access:record:create")
    public ApiResponse<Map<String, Long>> createAccessRecord(@Valid @RequestBody AccessRecordRequest request) {
        return ApiResponse.success(Map.of("recordId", service.createAccessRecord(request)));
    }

    @PostMapping("/api/app/visitors")
    @RequiresPermission("app:visitor:create")
    public ApiResponse<Map<String, Long>> createAppVisitor(@Valid @RequestBody VisitorInviteRequest request) {
        return ApiResponse.success(Map.of("visitorId", service.createVisitorInvite(request)));
    }
}
