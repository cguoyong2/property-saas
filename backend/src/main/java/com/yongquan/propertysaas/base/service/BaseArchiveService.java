package com.yongquan.propertysaas.base.service;

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
import com.yongquan.propertysaas.base.repository.BaseArchiveRepository;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BaseArchiveService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> HOUSE_STATUSES = Set.of("VACANT", "OCCUPIED", "RENTED", "LOCKED", "RENOVATING");

    private final BaseArchiveRepository repository;
    private final String projectCodePattern;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public BaseArchiveService(
            BaseArchiveRepository repository,
            @Value("${property-saas.project-code.pattern:SQ-{yyyyMMdd}-{projectId}}") String projectCodePattern) {
        this.repository = repository;
        this.projectCodePattern = projectCodePattern;
    }

    public PageResult<ProjectView> pageProjects(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findProjects(tenantId, scope, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countProjects(tenantId, scope, keyword, status),
                pageNo,
                pageSize);
    }

    public ProjectView getProject(Long projectId) {
        ensureProjectAllowed(projectId);
        return repository.getProject(tenantId(), projectId);
    }

    @Transactional
    public Long createProject(ProjectRequest request) {
        validateStatus(request.status());
        Long projectId = newId();
        String projectCode = hasText(request.projectCode()) ? request.projectCode().trim() : generateProjectCode(projectId);
        repository.insertProject(tenantId(), projectId, userId(), withProjectCode(request, projectCode));
        return projectId;
    }

    @Transactional
    public void updateProject(Long projectId, ProjectRequest request) {
        ensureProjectAllowed(projectId);
        validateStatus(request.status());
        repository.updateProject(tenantId(), projectId, userId(), request);
    }

    public PageResult<BuildingView> pageBuildings(Long projectId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findBuildings(tenantId, scope, projectId, offset(pageNo, pageSize), pageSize),
                repository.countBuildings(tenantId, scope, projectId),
                pageNo,
                pageSize);
    }

    public BuildingView getBuilding(Long buildingId) {
        BuildingView building = repository.getBuilding(tenantId(), buildingId);
        ensureProjectAllowed(building.projectId());
        return building;
    }

    @Transactional
    public Long createBuilding(BuildingRequest request) {
        validateBuilding(request);
        Long buildingId = newId();
        repository.insertBuilding(tenantId(), buildingId, userId(), request);
        return buildingId;
    }

    @Transactional
    public void updateBuilding(Long buildingId, BuildingRequest request) {
        getBuilding(buildingId);
        validateBuilding(request);
        repository.updateBuilding(tenantId(), buildingId, userId(), request);
    }

    public PageResult<UnitView> pageUnits(Long projectId, Long buildingId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findUnits(tenantId, scope, projectId, buildingId, offset(pageNo, pageSize), pageSize),
                repository.countUnits(tenantId, scope, projectId, buildingId),
                pageNo,
                pageSize);
    }

    public UnitView getUnit(Long unitId) {
        UnitView unit = repository.getUnit(tenantId(), unitId);
        ensureProjectAllowed(unit.projectId());
        return unit;
    }

    @Transactional
    public Long createUnit(UnitRequest request) {
        validateUnit(request);
        Long unitId = newId();
        repository.insertUnit(tenantId(), unitId, userId(), request);
        return unitId;
    }

    @Transactional
    public void updateUnit(Long unitId, UnitRequest request) {
        getUnit(unitId);
        validateUnit(request);
        repository.updateUnit(tenantId(), unitId, userId(), request);
    }

    public PageResult<HouseView> pageHouses(Long projectId, Long buildingId, Long unitId, String keyword,
                                            long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findHouses(tenantId, scope, projectId, buildingId, unitId, keyword, offset(pageNo, pageSize), pageSize),
                repository.countHouses(tenantId, scope, projectId, buildingId, unitId, keyword),
                pageNo,
                pageSize);
    }

    public HouseView getHouse(Long houseId) {
        HouseView house = repository.getHouse(tenantId(), houseId);
        ensureProjectAllowed(house.projectId());
        return house;
    }

    @Transactional
    public Long createHouse(HouseRequest request) {
        validateHouse(request, null);
        Long houseId = newId();
        repository.insertHouse(tenantId(), houseId, userId(), request);
        return houseId;
    }

    @Transactional
    public void updateHouse(Long houseId, HouseRequest request) {
        getHouse(houseId);
        validateHouse(request, houseId);
        repository.updateHouse(tenantId(), houseId, userId(), request);
    }

    @Transactional
    public ImportResultView importHouses(HouseImportRequest request) {
        Long tenantId = tenantId();
        ensureProjectAllowed(request.projectId());
        int total = request.rows().size();
        int success = 0;
        int fail = 0;
        Long batchId = newId();
        String batchNo = "HOUSE-" + batchId;
        for (int i = 0; i < request.rows().size(); i++) {
            HouseRequest row = request.rows().get(i);
            try {
                if (!request.projectId().equals(row.projectId())) {
                    throw new IllegalArgumentException("导入行项目ID必须等于批次项目ID");
                }
                validateHouse(row, null);
                repository.insertHouse(tenantId, newId(), userId(), row);
                success++;
            } catch (RuntimeException ex) {
                fail++;
                repository.insertImportError(tenantId, request.projectId(), batchId, newId(), i + 1, "houseNo",
                        row == null ? null : row.houseNo(), "HOUSE_IMPORT_ERROR", ex.getMessage());
            }
        }
        String status = fail == 0 ? "SUCCESS" : success == 0 ? "FAILED" : "PARTIAL_SUCCESS";
        repository.insertImportBatch(tenantId, request.projectId(), batchId, batchNo, request.sourceFileId(),
                total, success, fail, status, userId());
        return new ImportResultView(batchId, batchNo, total, success, fail, status);
    }

    private void validateBuilding(BuildingRequest request) {
        validateStatus(request.status());
        ensureProjectAllowed(request.projectId());
        if (!repository.projectExists(tenantId(), request.projectId())) {
            throw new IllegalArgumentException("项目不存在：" + request.projectId());
        }
    }

    private void validateUnit(UnitRequest request) {
        validateStatus(request.status());
        ensureProjectAllowed(request.projectId());
        if (!repository.buildingExists(tenantId(), request.projectId(), request.buildingId())) {
            throw new IllegalArgumentException("楼栋不存在或不属于项目：" + request.buildingId());
        }
    }

    private void validateHouse(HouseRequest request, Long excludeHouseId) {
        validateHouseStatus(request.houseStatus());
        ensureProjectAllowed(request.projectId());
        if (!repository.buildingExists(tenantId(), request.projectId(), request.buildingId())) {
            throw new IllegalArgumentException("楼栋不存在或不属于项目：" + request.buildingId());
        }
        if (!repository.unitExists(tenantId(), request.projectId(), request.buildingId(), request.unitId())) {
            throw new IllegalArgumentException("单元不存在或不属于楼栋：" + request.unitId());
        }
        if (repository.houseNoExists(tenantId(), request.projectId(), request.buildingId(), request.unitId(),
                request.houseNo(), excludeHouseId)) {
            throw new IllegalArgumentException("房号已存在：" + request.houseNo());
        }
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法状态：" + status);
        }
    }

    private void validateHouseStatus(String status) {
        if (status != null && !status.isBlank() && !HOUSE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法房屋状态：" + status);
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

    private ProjectRequest withProjectCode(ProjectRequest request, String projectCode) {
        return new ProjectRequest(
                projectCode,
                request.projectName(),
                request.projectType(),
                request.province(),
                request.city(),
                request.district(),
                request.address(),
                request.managerUserId(),
                request.servicePhone(),
                request.collectionSubject(),
                request.status());
    }

    private String generateProjectCode(Long projectId) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return projectCodePattern
                .replace("{yyyyMMdd}", date)
                .replace("{projectId}", String.valueOf(projectId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
