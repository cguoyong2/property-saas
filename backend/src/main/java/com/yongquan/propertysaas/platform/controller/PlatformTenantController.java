package com.yongquan.propertysaas.platform.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.platform.domain.DashboardView;
import com.yongquan.propertysaas.platform.domain.PlatformMonitorView;
import com.yongquan.propertysaas.platform.domain.PackageView;
import com.yongquan.propertysaas.platform.domain.TenantConfigView;
import com.yongquan.propertysaas.platform.domain.TenantSummary;
import com.yongquan.propertysaas.platform.domain.UsageView;
import com.yongquan.propertysaas.platform.dto.PackageRequest;
import com.yongquan.propertysaas.platform.dto.TenantConfigRequest;
import com.yongquan.propertysaas.platform.dto.TenantRequest;
import com.yongquan.propertysaas.platform.dto.TenantStatusRequest;
import com.yongquan.propertysaas.platform.service.PlatformTenantService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformTenantController {

    private final PlatformTenantService service;

    public PlatformTenantController(PlatformTenantService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @RequiresPermission(value = "platform:dashboard:view", platformOnly = true)
    public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.success(service.dashboard());
    }

    @GetMapping("/monitor")
    @RequiresPermission(value = "platform:monitor:view", platformOnly = true)
    public ApiResponse<PlatformMonitorView> monitor() {
        return ApiResponse.success(service.monitor());
    }

    @GetMapping("/tenants")
    @RequiresPermission(value = "platform:tenant:list", platformOnly = true)
    public ApiResponse<PageResult<TenantSummary>> tenants(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(service.pageTenants(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/tenants")
    @RequiresPermission(value = "platform:tenant:create", platformOnly = true)
    public ApiResponse<java.util.Map<String, Long>> createTenant(@Valid @RequestBody TenantRequest request) {
        return ApiResponse.success(java.util.Map.of("tenantId", service.createTenant(request)));
    }

    @GetMapping("/tenants/{tenantId}")
    @RequiresPermission(value = "platform:tenant:view", platformOnly = true)
    public ApiResponse<TenantSummary> tenant(@PathVariable Long tenantId) {
        return ApiResponse.success(service.getTenant(tenantId));
    }

    @PutMapping("/tenants/{tenantId}")
    @RequiresPermission(value = "platform:tenant:update", platformOnly = true)
    public ApiResponse<Void> updateTenant(@PathVariable Long tenantId, @Valid @RequestBody TenantRequest request) {
        service.updateTenant(tenantId, request);
        return ApiResponse.success();
    }

    @PutMapping("/tenants/{tenantId}/status")
    @RequiresPermission(value = "platform:tenant:status", platformOnly = true)
    public ApiResponse<Void> updateStatus(@PathVariable Long tenantId, @Valid @RequestBody TenantStatusRequest request) {
        service.updateStatus(tenantId, request);
        return ApiResponse.success();
    }

    @GetMapping("/tenants/{tenantId}/config")
    @RequiresPermission(value = "platform:tenant:config:view", platformOnly = true)
    public ApiResponse<TenantConfigView> config(@PathVariable Long tenantId) {
        return ApiResponse.success(service.getConfig(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/config")
    @RequiresPermission(value = "platform:tenant:config:update", platformOnly = true)
    public ApiResponse<Void> updateConfig(@PathVariable Long tenantId, @RequestBody TenantConfigRequest request) {
        service.updateConfig(tenantId, request);
        return ApiResponse.success();
    }

    @GetMapping("/packages")
    @RequiresPermission(value = "platform:package:list", platformOnly = true)
    public ApiResponse<PageResult<PackageView>> packages(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePackages(pageNo, pageSize));
    }

    @PostMapping("/packages")
    @RequiresPermission(value = "platform:package:create", platformOnly = true)
    public ApiResponse<java.util.Map<String, Long>> createPackage(@Valid @RequestBody PackageRequest request) {
        return ApiResponse.success(java.util.Map.of("packageId", service.createPackage(request)));
    }

    @GetMapping("/usage")
    @RequiresPermission(value = "platform:usage:view", platformOnly = true)
    public ApiResponse<PageResult<UsageView>> usage(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageUsage(pageNo, pageSize));
    }
}
