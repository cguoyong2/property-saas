package com.yongquan.propertysaas.vehicle.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.vehicle.domain.ParkingSpaceView;
import com.yongquan.propertysaas.vehicle.domain.ParkingSyncRecordView;
import com.yongquan.propertysaas.vehicle.domain.VehicleView;
import com.yongquan.propertysaas.vehicle.dto.MonthlyRentRequest;
import com.yongquan.propertysaas.vehicle.dto.ParkingSpaceRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleRequest;
import com.yongquan.propertysaas.vehicle.service.VehicleService;
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
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping("/api/base/parking-spaces")
    @RequiresPermission("base:parkingSpace:list")
    public ApiResponse<PageResult<ParkingSpaceView>> pageSpaces(@RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageSpaces(projectId, keyword, status, pageNo, pageSize));
    }

    @PostMapping("/api/base/parking-spaces")
    @RequiresPermission("base:parkingSpace:create")
    public ApiResponse<Map<String, Long>> createSpace(@Valid @RequestBody ParkingSpaceRequest request) {
        return ApiResponse.success(Map.of("spaceId", service.createSpace(request)));
    }

    @GetMapping("/api/base/parking-spaces/{spaceId}")
    @RequiresPermission("base:parkingSpace:view")
    public ApiResponse<ParkingSpaceView> getSpace(@PathVariable Long spaceId) {
        return ApiResponse.success(service.getSpace(spaceId));
    }

    @PutMapping("/api/base/parking-spaces/{spaceId}")
    @RequiresPermission("base:parkingSpace:update")
    public ApiResponse<Void> updateSpace(@PathVariable Long spaceId, @Valid @RequestBody ParkingSpaceRequest request) {
        service.updateSpace(spaceId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/base/vehicles")
    @RequiresPermission("base:vehicle:list")
    public ApiResponse<PageResult<VehicleView>> pageVehicles(@RequestParam(required = false) Long projectId,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") long pageNo,
                                                             @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageVehicles(projectId, keyword, status, pageNo, pageSize));
    }

    @PostMapping("/api/base/vehicles")
    @RequiresPermission("base:vehicle:create")
    public ApiResponse<Map<String, Long>> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ApiResponse.success(Map.of("vehicleId", service.createVehicle(request)));
    }

    @GetMapping("/api/base/vehicles/{vehicleId}")
    @RequiresPermission("base:vehicle:view")
    public ApiResponse<VehicleView> getVehicle(@PathVariable Long vehicleId) {
        return ApiResponse.success(service.getVehicle(vehicleId));
    }

    @PutMapping("/api/base/vehicles/{vehicleId}")
    @RequiresPermission("base:vehicle:update")
    public ApiResponse<Void> updateVehicle(@PathVariable Long vehicleId, @Valid @RequestBody VehicleRequest request) {
        service.updateVehicle(vehicleId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/base/vehicles/{vehicleId}/monthly-rent")
    @RequiresPermission("base:vehicle:monthlyRent")
    public ApiResponse<Void> updateMonthlyRent(@PathVariable Long vehicleId, @Valid @RequestBody MonthlyRentRequest request) {
        service.updateMonthlyRent(vehicleId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/device/parking")
    @RequiresPermission("device:parking:list")
    public ApiResponse<PageResult<ParkingSyncRecordView>> pageParkingSyncRecords(@RequestParam(required = false) Long projectId,
                                                                                 @RequestParam(required = false) String status,
                                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageParkingSyncRecords(projectId, status, pageNo, pageSize));
    }
}
