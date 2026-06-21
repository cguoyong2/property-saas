package com.yongquan.propertysaas.vehicle.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.vehicle.domain.VehicleBrandView;
import com.yongquan.propertysaas.vehicle.domain.VehicleModelView;
import com.yongquan.propertysaas.vehicle.dto.VehicleBrandRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleModelRequest;
import com.yongquan.propertysaas.vehicle.service.VehicleCatalogService;
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
public class VehicleCatalogController {

    private final VehicleCatalogService service;

    public VehicleCatalogController(VehicleCatalogService service) {
        this.service = service;
    }

    @GetMapping("/api/base/vehicle-brands")
    @RequiresPermission("base:vehicle:list")
    public ApiResponse<PageResult<VehicleBrandView>> pageBrands(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageBrands(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/api/base/vehicle-brands")
    @RequiresPermission("base:vehicle:update")
    public ApiResponse<Map<String, Long>> createBrand(@Valid @RequestBody VehicleBrandRequest request) {
        return ApiResponse.success(Map.of("brandId", service.createBrand(request)));
    }

    @PutMapping("/api/base/vehicle-brands/{brandId}")
    @RequiresPermission("base:vehicle:update")
    public ApiResponse<Void> updateBrand(@PathVariable Long brandId, @Valid @RequestBody VehicleBrandRequest request) {
        service.updateBrand(brandId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/base/vehicle-models")
    @RequiresPermission("base:vehicle:list")
    public ApiResponse<PageResult<VehicleModelView>> pageModels(@RequestParam(required = false) Long brandId,
                                                                @RequestParam(required = false) String brandName,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageModels(brandId, brandName, keyword, status, pageNo, pageSize));
    }

    @PostMapping("/api/base/vehicle-models")
    @RequiresPermission("base:vehicle:update")
    public ApiResponse<Map<String, Long>> createModel(@Valid @RequestBody VehicleModelRequest request) {
        return ApiResponse.success(Map.of("modelId", service.createModel(request)));
    }

    @PutMapping("/api/base/vehicle-models/{modelId}")
    @RequiresPermission("base:vehicle:update")
    public ApiResponse<Void> updateModel(@PathVariable Long modelId, @Valid @RequestBody VehicleModelRequest request) {
        service.updateModel(modelId, request);
        return ApiResponse.success();
    }
}
