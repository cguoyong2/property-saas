package com.yongquan.propertysaas.base.controller;

import com.yongquan.propertysaas.base.domain.BuildingView;
import com.yongquan.propertysaas.base.domain.HouseView;
import com.yongquan.propertysaas.base.domain.ImportResultView;
import com.yongquan.propertysaas.base.domain.ProjectView;
import com.yongquan.propertysaas.base.domain.UnitView;
import com.yongquan.propertysaas.base.dto.BuildingRequest;
import com.yongquan.propertysaas.base.dto.HouseImportRequest;
import com.yongquan.propertysaas.base.dto.HouseRequest;
import com.yongquan.propertysaas.base.dto.ProjectRequest;
import com.yongquan.propertysaas.base.dto.UnitRequest;
import com.yongquan.propertysaas.base.service.BaseArchiveService;
import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base")
public class BaseArchiveController {

    private final BaseArchiveService service;

    public BaseArchiveController(BaseArchiveService service) {
        this.service = service;
    }

    @GetMapping("/projects")
    @RequiresPermission("base:project:list")
    public ApiResponse<PageResult<ProjectView>> pageProjects(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") long pageNo,
                                                             @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageProjects(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/projects")
    @RequiresPermission("base:project:create")
    public ApiResponse<Map<String, Long>> createProject(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(Map.of("projectId", service.createProject(request)));
    }

    @GetMapping("/projects/{projectId}")
    @RequiresPermission("base:project:view")
    public ApiResponse<ProjectView> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(service.getProject(projectId));
    }

    @PutMapping("/projects/{projectId}")
    @RequiresPermission("base:project:update")
    public ApiResponse<Void> updateProject(@PathVariable Long projectId, @Valid @RequestBody ProjectRequest request) {
        service.updateProject(projectId, request);
        return ApiResponse.success();
    }

    @GetMapping("/buildings")
    @RequiresPermission("base:building:list")
    public ApiResponse<PageResult<BuildingView>> pageBuildings(@RequestParam(required = false) Long projectId,
                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageBuildings(projectId, pageNo, pageSize));
    }

    @PostMapping("/buildings")
    @RequiresPermission("base:building:create")
    public ApiResponse<Map<String, Long>> createBuilding(@Valid @RequestBody BuildingRequest request) {
        return ApiResponse.success(Map.of("buildingId", service.createBuilding(request)));
    }

    @GetMapping("/buildings/{buildingId}")
    @RequiresPermission("base:building:view")
    public ApiResponse<BuildingView> getBuilding(@PathVariable Long buildingId) {
        return ApiResponse.success(service.getBuilding(buildingId));
    }

    @PutMapping("/buildings/{buildingId}")
    @RequiresPermission("base:building:update")
    public ApiResponse<Void> updateBuilding(@PathVariable Long buildingId, @Valid @RequestBody BuildingRequest request) {
        service.updateBuilding(buildingId, request);
        return ApiResponse.success();
    }

    @GetMapping("/units")
    @RequiresPermission("base:unit:list")
    public ApiResponse<PageResult<UnitView>> pageUnits(@RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) Long buildingId,
                                                       @RequestParam(defaultValue = "1") long pageNo,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageUnits(projectId, buildingId, pageNo, pageSize));
    }

    @PostMapping("/units")
    @RequiresPermission("base:unit:create")
    public ApiResponse<Map<String, Long>> createUnit(@Valid @RequestBody UnitRequest request) {
        return ApiResponse.success(Map.of("unitId", service.createUnit(request)));
    }

    @GetMapping("/units/{unitId}")
    @RequiresPermission("base:unit:view")
    public ApiResponse<UnitView> getUnit(@PathVariable Long unitId) {
        return ApiResponse.success(service.getUnit(unitId));
    }

    @PutMapping("/units/{unitId}")
    @RequiresPermission("base:unit:update")
    public ApiResponse<Void> updateUnit(@PathVariable Long unitId, @Valid @RequestBody UnitRequest request) {
        service.updateUnit(unitId, request);
        return ApiResponse.success();
    }

    @GetMapping("/houses")
    @RequiresPermission("base:house:list")
    public ApiResponse<PageResult<HouseView>> pageHouses(@RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) Long buildingId,
                                                         @RequestParam(required = false) Long unitId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") long pageNo,
                                                         @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageHouses(projectId, buildingId, unitId, keyword, pageNo, pageSize));
    }

    @PostMapping("/houses")
    @RequiresPermission("base:house:create")
    public ApiResponse<Map<String, Long>> createHouse(@Valid @RequestBody HouseRequest request) {
        return ApiResponse.success(Map.of("houseId", service.createHouse(request)));
    }

    @GetMapping("/houses/{houseId}")
    @RequiresPermission("base:house:view")
    public ApiResponse<HouseView> getHouse(@PathVariable Long houseId) {
        return ApiResponse.success(service.getHouse(houseId));
    }

    @PutMapping("/houses/{houseId}")
    @RequiresPermission("base:house:update")
    public ApiResponse<Void> updateHouse(@PathVariable Long houseId, @Valid @RequestBody HouseRequest request) {
        service.updateHouse(houseId, request);
        return ApiResponse.success();
    }

    @PostMapping("/houses/import")
    @RequiresPermission("base:house:import")
    public ApiResponse<ImportResultView> importHouses(@Valid @RequestBody HouseImportRequest request) {
        return ApiResponse.success(service.importHouses(request));
    }
}
