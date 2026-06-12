package com.yongquan.propertysaas.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.platform.domain.DashboardView;
import com.yongquan.propertysaas.platform.domain.PackageView;
import com.yongquan.propertysaas.platform.domain.PlatformMonitorView;
import com.yongquan.propertysaas.platform.domain.TenantConfigView;
import com.yongquan.propertysaas.platform.domain.TenantSummary;
import com.yongquan.propertysaas.platform.domain.UsageView;
import com.yongquan.propertysaas.platform.dto.PackageRequest;
import com.yongquan.propertysaas.platform.dto.TenantConfigRequest;
import com.yongquan.propertysaas.platform.dto.TenantRequest;
import com.yongquan.propertysaas.platform.dto.TenantStatusRequest;
import com.yongquan.propertysaas.platform.repository.PlatformTenantRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformTenantService {

    private static final Set<String> TENANT_STATUSES = Set.of(
            "PENDING", "TRIAL", "ACTIVE", "ARREARS_LIMITED", "SUSPENDED", "ARCHIVED"
    );

    private final PlatformTenantRepository repository;
    private final ObjectMapper objectMapper;

    public PlatformTenantService(PlatformTenantRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public DashboardView dashboard() {
        return repository.dashboard();
    }

    public PlatformMonitorView monitor() {
        return repository.monitor();
    }

    public PageResult<TenantSummary> pageTenants(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        List<TenantSummary> records = repository.findTenants(keyword, status, offset(pageNo, pageSize), pageSize);
        return new PageResult<>(records, repository.countTenants(keyword, status), pageNo, pageSize);
    }

    public TenantSummary getTenant(Long tenantId) {
        return repository.getTenant(tenantId);
    }

    @Transactional
    public Long createTenant(TenantRequest request) {
        ensurePackageExists(request.packageId());
        Long tenantId = newId();
        repository.insertTenant(tenantId, request);
        repository.upsertTenantConfig(tenantId, new TenantConfigRequest(null, null, null, null, null, "SHARED_BUCKET"));
        return tenantId;
    }

    @Transactional
    public void updateTenant(Long tenantId, TenantRequest request) {
        ensurePackageExists(request.packageId());
        repository.updateTenant(tenantId, request);
    }

    @Transactional
    public void updateStatus(Long tenantId, TenantStatusRequest request) {
        if (!TENANT_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("非法租户状态：" + request.status());
        }
        repository.updateTenantStatus(tenantId, request.status(), request.reason());
    }

    public TenantConfigView getConfig(Long tenantId) {
        return repository.getTenantConfig(tenantId);
    }

    @Transactional
    public void updateConfig(Long tenantId, TenantConfigRequest request) {
        repository.upsertTenantConfig(tenantId, request);
    }

    public PageResult<PackageView> pagePackages(long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        List<PackageView> records = repository.findPackages(offset(pageNo, pageSize), pageSize);
        return new PageResult<>(records, repository.countPackages(), pageNo, pageSize);
    }

    @Transactional
    public Long createPackage(PackageRequest request) {
        Long packageId = newId();
        repository.insertPackage(packageId, request, toJsonArray(request.enabledModules()), toJsonObject(request.quotas()));
        return packageId;
    }

    public PageResult<UsageView> pageUsage(long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        List<UsageView> records = repository.findUsage(offset(pageNo, pageSize), pageSize);
        return new PageResult<>(records, repository.countUsage(), pageNo, pageSize);
    }

    private void ensurePackageExists(Long packageId) {
        if (!repository.packageExists(packageId)) {
            throw new IllegalArgumentException("套餐不存在：" + packageId);
        }
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
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }

    private String toJsonArray(Object value) {
        return toJson(value == null ? List.of() : value);
    }

    private String toJsonObject(Object value) {
        return toJson(value == null ? java.util.Map.of() : value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON序列化失败", ex);
        }
    }
}
