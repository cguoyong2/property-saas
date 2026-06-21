package com.yongquan.propertysaas.vehicle.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import com.yongquan.propertysaas.vehicle.domain.VehicleBrandView;
import com.yongquan.propertysaas.vehicle.domain.VehicleModelView;
import com.yongquan.propertysaas.vehicle.dto.VehicleBrandRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleModelRequest;
import com.yongquan.propertysaas.vehicle.repository.VehicleCatalogRepository;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleCatalogService {

    private static final Set<String> ENABLED_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final VehicleCatalogRepository repository;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public VehicleCatalogService(VehicleCatalogRepository repository) {
        this.repository = repository;
    }

    public PageResult<VehicleBrandView> pageBrands(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findBrands(tenantId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countBrands(tenantId, keyword, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createBrand(VehicleBrandRequest request) {
        validateBrand(request, null);
        Long brandId = newId();
        repository.insertBrand(tenantId(), brandId, userId(), request);
        return brandId;
    }

    @Transactional
    public void updateBrand(Long brandId, VehicleBrandRequest request) {
        repository.getBrand(tenantId(), brandId);
        validateBrand(request, brandId);
        repository.updateBrand(tenantId(), brandId, userId(), request);
    }

    public PageResult<VehicleModelView> pageModels(Long brandId, String brandName, String keyword,
                                                   String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findModels(tenantId, brandId, brandName, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countModels(tenantId, brandId, brandName, keyword, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createModel(VehicleModelRequest request) {
        validateModel(request, null);
        Long modelId = newId();
        repository.insertModel(tenantId(), modelId, userId(), request);
        return modelId;
    }

    @Transactional
    public void updateModel(Long modelId, VehicleModelRequest request) {
        repository.getModel(tenantId(), modelId);
        validateModel(request, modelId);
        repository.updateModel(tenantId(), modelId, userId(), request);
    }

    private void validateBrand(VehicleBrandRequest request, Long excludeBrandId) {
        if (request.brandName() == null || request.brandName().isBlank()) {
            throw new IllegalArgumentException("车辆品牌不能为空");
        }
        validateEnabledStatus(request.status());
        if (repository.brandNameExists(tenantId(), request.brandName(), excludeBrandId)) {
            throw new IllegalArgumentException("车辆品牌已存在：" + request.brandName().trim());
        }
    }

    private void validateModel(VehicleModelRequest request, Long excludeModelId) {
        if (request.brandId() == null || !repository.brandExists(tenantId(), request.brandId())) {
            throw new IllegalArgumentException("车辆品牌不存在或已停用：" + request.brandId());
        }
        if (request.modelName() == null || request.modelName().isBlank()) {
            throw new IllegalArgumentException("车辆型号不能为空");
        }
        validateEnabledStatus(request.status());
        if (repository.modelNameExists(tenantId(), request.brandId(), request.modelName(), excludeModelId)) {
            throw new IllegalArgumentException("该品牌下车辆型号已存在：" + request.modelName().trim());
        }
    }

    private void validateEnabledStatus(String status) {
        if (status != null && !status.isBlank() && !ENABLED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法状态：" + status);
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

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
