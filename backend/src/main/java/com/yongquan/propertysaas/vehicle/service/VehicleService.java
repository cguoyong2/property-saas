package com.yongquan.propertysaas.vehicle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import com.yongquan.propertysaas.vehicle.domain.ParkingAreaView;
import com.yongquan.propertysaas.vehicle.domain.ParkingSpaceView;
import com.yongquan.propertysaas.vehicle.domain.ParkingSyncRecordView;
import com.yongquan.propertysaas.vehicle.domain.VehicleView;
import com.yongquan.propertysaas.vehicle.dto.MonthlyRentRequest;
import com.yongquan.propertysaas.vehicle.dto.ParkingAreaRequest;
import com.yongquan.propertysaas.vehicle.dto.ParkingSpaceRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleRequest;
import com.yongquan.propertysaas.vehicle.repository.VehicleRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private static final Set<String> SPACE_STATUSES = Set.of("AVAILABLE", "OCCUPIED", "LOCKED", "DISABLED");
    private static final Set<String> AREA_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> VEHICLE_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> RENT_STATUSES = Set.of("NONE", "ACTIVE", "EXPIRED", "SUSPENDED");

    private final VehicleRepository repository;
    private final ObjectMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public VehicleService(VehicleRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public PageResult<ParkingAreaView> pageAreas(Long projectId, String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findAreas(tenantId, scope, projectId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countAreas(tenantId, scope, projectId, keyword, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createArea(ParkingAreaRequest request) {
        validateArea(request);
        Long areaId = newId();
        repository.insertArea(tenantId(), areaId, userId(), request);
        return areaId;
    }

    @Transactional
    public void updateArea(Long areaId, ParkingAreaRequest request) {
        ParkingAreaView area = repository.getArea(tenantId(), areaId);
        ensureProjectAllowed(area.projectId());
        validateArea(request);
        repository.updateArea(tenantId(), areaId, userId(), request);
    }

    public PageResult<ParkingSpaceView> pageSpaces(Long projectId, String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findSpaces(tenantId, scope, projectId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countSpaces(tenantId, scope, projectId, keyword, status),
                pageNo,
                pageSize);
    }

    public ParkingSpaceView getSpace(Long spaceId) {
        ParkingSpaceView space = repository.getSpace(tenantId(), spaceId);
        ensureProjectAllowed(space.projectId());
        return space;
    }

    @Transactional
    public Long createSpace(ParkingSpaceRequest request) {
        validateSpace(request);
        Long spaceId = newId();
        repository.insertSpace(tenantId(), spaceId, userId(), request);
        return spaceId;
    }

    @Transactional
    public void updateSpace(Long spaceId, ParkingSpaceRequest request) {
        getSpace(spaceId);
        validateSpace(request);
        repository.updateSpace(tenantId(), spaceId, userId(), request);
    }

    public PageResult<VehicleView> pageVehicles(Long projectId, String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findVehicles(tenantId, scope, projectId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countVehicles(tenantId, scope, projectId, keyword, status),
                pageNo,
                pageSize);
    }

    public VehicleView getVehicle(Long vehicleId) {
        VehicleView vehicle = repository.getVehicle(tenantId(), vehicleId);
        ensureProjectAllowed(vehicle.projectId());
        return vehicle;
    }

    @Transactional
    public Long createVehicle(VehicleRequest request) {
        validateVehicle(request, null);
        Long vehicleId = newId();
        repository.insertVehicle(tenantId(), vehicleId, userId(), request);
        createSync(request.projectId(), request.plateNo(), "VEHICLE_UPSERT", Map.of("vehicleId", vehicleId, "action", "create"));
        return vehicleId;
    }

    @Transactional
    public void updateVehicle(Long vehicleId, VehicleRequest request) {
        getVehicle(vehicleId);
        validateVehicle(request, vehicleId);
        repository.updateVehicle(tenantId(), vehicleId, userId(), request);
        createSync(request.projectId(), request.plateNo(), "VEHICLE_UPSERT", Map.of("vehicleId", vehicleId, "action", "update"));
    }

    @Transactional
    public void updateMonthlyRent(Long vehicleId, MonthlyRentRequest request) {
        VehicleView vehicle = getVehicle(vehicleId);
        validateRentStatus(request.monthlyRentStatus());
        repository.updateMonthlyRent(tenantId(), vehicleId, userId(), request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("vehicleId", vehicleId);
        payload.put("monthlyRentStatus", request.monthlyRentStatus());
        payload.put("startDate", request.startDate());
        payload.put("endDate", request.endDate());
        createSync(vehicle.projectId(), vehicle.plateNo(), "MONTHLY_RENT", payload);
    }

    public PageResult<ParkingSyncRecordView> pageParkingSyncRecords(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findParkingSyncRecords(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countParkingSyncRecords(tenantId, scope, projectId, status),
                pageNo,
                pageSize);
    }

    private void validateSpace(ParkingSpaceRequest request) {
        ensureProjectAllowed(request.projectId());
        validateSpaceStatus(request.status());
        if (!repository.areaExists(tenantId(), request.projectId(), request.areaId())) {
            throw new IllegalArgumentException("车位区域不存在或不属于小区：" + request.areaId());
        }
        if (!repository.houseHierarchyExists(tenantId(), request.projectId(), request.buildingId(), request.unitId(), request.houseId())) {
            throw new IllegalArgumentException("房屋不存在或不属于所选小区、楼栋、单元");
        }
    }

    private void validateArea(ParkingAreaRequest request) {
        ensureProjectAllowed(request.projectId());
        validateAreaStatus(request.status());
    }

    private void validateVehicle(VehicleRequest request, Long excludeVehicleId) {
        ensureProjectAllowed(request.projectId());
        validateVehicleStatus(request.status());
        validateRentStatus(request.monthlyRentStatus());
        if (!repository.memberExists(tenantId(), request.memberId())) {
            throw new IllegalArgumentException("会员不存在：" + request.memberId());
        }
        if (!repository.houseExists(tenantId(), request.projectId(), request.houseId())) {
            throw new IllegalArgumentException("房屋不存在或不属于项目：" + request.houseId());
        }
        if (!repository.spaceExists(tenantId(), request.projectId(), request.spaceId())) {
            throw new IllegalArgumentException("车位不存在或不属于项目：" + request.spaceId());
        }
        if (repository.plateExists(tenantId(), request.projectId(), request.plateNo(), excludeVehicleId)) {
            throw new IllegalArgumentException("车牌号已存在：" + request.plateNo());
        }
    }

    private void createSync(Long projectId, String plateNo, String syncType, Map<String, Object> requestData) {
        repository.insertParkingSyncRecord(newId(), tenantId(), projectId, "RESERVED", plateNo, syncType, toJson(requestData));
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("停车同步数据序列化失败", ex);
        }
    }

    private void validateSpaceStatus(String status) {
        if (status != null && !status.isBlank() && !SPACE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法车位状态：" + status);
        }
    }

    private void validateAreaStatus(String status) {
        if (status != null && !status.isBlank() && !AREA_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法车位区域状态：" + status);
        }
    }

    private void validateVehicleStatus(String status) {
        if (status != null && !status.isBlank() && !VEHICLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法车辆状态：" + status);
        }
    }

    private void validateRentStatus(String status) {
        if (status != null && !status.isBlank() && !RENT_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法月租状态：" + status);
        }
    }

    private void ensureProjectAllowed(Long projectId) {
        if (!repository.projectExists(tenantId(), projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
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
        return idSequence.incrementAndGet();
    }
}
