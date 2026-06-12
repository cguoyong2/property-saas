package com.yongquan.propertysaas.base.repository;

import com.yongquan.propertysaas.base.domain.BuildingView;
import com.yongquan.propertysaas.base.domain.HouseView;
import com.yongquan.propertysaas.base.domain.ProjectView;
import com.yongquan.propertysaas.base.domain.UnitView;
import com.yongquan.propertysaas.base.dto.BuildingRequest;
import com.yongquan.propertysaas.base.dto.HouseRequest;
import com.yongquan.propertysaas.base.dto.ProjectRequest;
import com.yongquan.propertysaas.base.dto.UnitRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BaseArchiveRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public BaseArchiveRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<ProjectView> findProjects(Long tenantId, List<Long> allowedProjectIds, String keyword, String status,
                                          long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT project_id, project_code, project_name, project_type, province, city, district, address,
                       manager_user_id, service_phone, collection_subject, status, created_at
                FROM base_project
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectScope(sql, args, allowedProjectIds);
        appendProjectFilters(sql, args, keyword, status);
        sql.append(" ORDER BY created_at DESC, project_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapProject, args.toArray());
    }

    public long countProjects(Long tenantId, List<Long> allowedProjectIds, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectScope(sql, args, allowedProjectIds);
        appendProjectFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public ProjectView getProject(Long tenantId, Long projectId) {
        return jdbcTemplate.queryForObject("""
                SELECT project_id, project_code, project_name, project_type, province, city, district, address,
                       manager_user_id, service_phone, collection_subject, status, created_at
                FROM base_project
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0
                """, this::mapProject, tenantId, projectId);
    }

    public void insertProject(Long tenantId, Long projectId, Long userId, ProjectRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_project(project_id, tenant_id, project_code, project_name, project_type,
                                                 province, city, district, address, manager_user_id, service_phone,
                                                 collection_subject, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                projectId, tenantId, request.projectCode(), request.projectName(), text(request.projectType(), "RESIDENTIAL"),
                request.province(), request.city(), request.district(), request.address(), request.managerUserId(),
                request.servicePhone(), request.collectionSubject(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateProject(Long tenantId, Long projectId, Long userId, ProjectRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_project
                        SET project_name = ?, project_type = ?, province = ?, city = ?, district = ?, address = ?,
                            manager_user_id = ?, service_phone = ?, collection_subject = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND project_id = ? AND deleted = 0
                        """,
                request.projectName(), text(request.projectType(), "RESIDENTIAL"), request.province(), request.city(),
                request.district(), request.address(), request.managerUserId(), request.servicePhone(),
                request.collectionSubject(), text(request.status(), "ACTIVE"), userId, tenantId, projectId);
    }

    public List<BuildingView> findBuildings(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                            long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT building_id, project_id, building_name, building_code, building_type, floor_count,
                       sort_no, status, created_at
                FROM base_building
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY project_id ASC, sort_no ASC, building_id ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBuilding, args.toArray());
    }

    public long countBuildings(Long tenantId, List<Long> allowedProjectIds, Long projectId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_building WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public BuildingView getBuilding(Long tenantId, Long buildingId) {
        return jdbcTemplate.queryForObject("""
                SELECT building_id, project_id, building_name, building_code, building_type, floor_count,
                       sort_no, status, created_at
                FROM base_building
                WHERE tenant_id = ? AND building_id = ? AND deleted = 0
                """, this::mapBuilding, tenantId, buildingId);
    }

    public void insertBuilding(Long tenantId, Long buildingId, Long userId, BuildingRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_building(building_id, tenant_id, project_id, building_name, building_code,
                                                  building_type, floor_count, sort_no, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                buildingId, tenantId, request.projectId(), request.buildingName(), request.buildingCode(),
                text(request.buildingType(), "BUILDING"), request.floorCount(), intValue(request.sortNo()),
                text(request.status(), "ACTIVE"), userId);
    }

    public void updateBuilding(Long tenantId, Long buildingId, Long userId, BuildingRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_building
                        SET project_id = ?, building_name = ?, building_code = ?, building_type = ?, floor_count = ?,
                            sort_no = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND building_id = ? AND deleted = 0
                        """,
                request.projectId(), request.buildingName(), request.buildingCode(), text(request.buildingType(), "BUILDING"),
                request.floorCount(), intValue(request.sortNo()), text(request.status(), "ACTIVE"), userId, tenantId, buildingId);
    }

    public List<UnitView> findUnits(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long buildingId,
                                    long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT unit_id, project_id, building_id, unit_name, sort_no, status, created_at
                FROM base_unit
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (buildingId != null) {
            sql.append(" AND building_id = ?");
            args.add(buildingId);
        }
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY project_id ASC, building_id ASC, sort_no ASC, unit_id ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapUnit, args.toArray());
    }

    public long countUnits(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long buildingId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_unit WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (buildingId != null) {
            sql.append(" AND building_id = ?");
            args.add(buildingId);
        }
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public UnitView getUnit(Long tenantId, Long unitId) {
        return jdbcTemplate.queryForObject("""
                SELECT unit_id, project_id, building_id, unit_name, sort_no, status, created_at
                FROM base_unit
                WHERE tenant_id = ? AND unit_id = ? AND deleted = 0
                """, this::mapUnit, tenantId, unitId);
    }

    public void insertUnit(Long tenantId, Long unitId, Long userId, UnitRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_unit(unit_id, tenant_id, project_id, building_id, unit_name, sort_no, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                unitId, tenantId, request.projectId(), request.buildingId(), request.unitName(), intValue(request.sortNo()),
                text(request.status(), "ACTIVE"), userId);
    }

    public void updateUnit(Long tenantId, Long unitId, Long userId, UnitRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_unit
                        SET project_id = ?, building_id = ?, unit_name = ?, sort_no = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND unit_id = ? AND deleted = 0
                        """,
                request.projectId(), request.buildingId(), request.unitName(), intValue(request.sortNo()),
                text(request.status(), "ACTIVE"), userId, tenantId, unitId);
    }

    public List<HouseView> findHouses(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long buildingId,
                                      Long unitId, String keyword, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT house_id, project_id, building_id, unit_id, house_no, floor_no, building_area, inner_area,
                       house_usage, house_status, charge_object, created_at
                FROM base_house
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendHouseFilters(sql, args, projectId, buildingId, unitId, keyword);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY project_id ASC, building_id ASC, unit_id ASC, house_no ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapHouse, args.toArray());
    }

    public long countHouses(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long buildingId,
                            Long unitId, String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendHouseFilters(sql, args, projectId, buildingId, unitId, keyword);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public HouseView getHouse(Long tenantId, Long houseId) {
        return jdbcTemplate.queryForObject("""
                SELECT house_id, project_id, building_id, unit_id, house_no, floor_no, building_area, inner_area,
                       house_usage, house_status, charge_object, created_at
                FROM base_house
                WHERE tenant_id = ? AND house_id = ? AND deleted = 0
                """, this::mapHouse, tenantId, houseId);
    }

    public void insertHouse(Long tenantId, Long houseId, Long userId, HouseRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_house(house_id, tenant_id, project_id, building_id, unit_id, house_no,
                                               floor_no, building_area, inner_area, house_usage, house_status,
                                               charge_object, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                houseId, tenantId, request.projectId(), request.buildingId(), request.unitId(), request.houseNo(),
                request.floorNo(), request.buildingArea(), request.innerArea(), text(request.houseUsage(), "RESIDENTIAL"),
                text(request.houseStatus(), "VACANT"), text(request.chargeObject(), "HOUSE"), userId);
    }

    public void updateHouse(Long tenantId, Long houseId, Long userId, HouseRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_house
                        SET project_id = ?, building_id = ?, unit_id = ?, house_no = ?, floor_no = ?,
                            building_area = ?, inner_area = ?, house_usage = ?, house_status = ?,
                            charge_object = ?, updated_by = ?
                        WHERE tenant_id = ? AND house_id = ? AND deleted = 0
                        """,
                request.projectId(), request.buildingId(), request.unitId(), request.houseNo(), request.floorNo(),
                request.buildingArea(), request.innerArea(), text(request.houseUsage(), "RESIDENTIAL"),
                text(request.houseStatus(), "VACANT"), text(request.chargeObject(), "HOUSE"), userId, tenantId, houseId);
    }

    public Long insertImportBatch(Long tenantId, Long projectId, Long batchId, String batchNo, Long sourceFileId,
                                  int totalCount, int successCount, int failCount, String status, Long userId) {
        jdbcTemplate.update("""
                        INSERT INTO import_batch(batch_id, tenant_id, project_id, import_type, batch_no, source_file_id,
                                                 total_count, success_count, fail_count, import_status, can_rollback,
                                                 rollback_status, created_by)
                        VALUES (?, ?, ?, 'HOUSE', ?, ?, ?, ?, ?, ?, 0, 'NONE', ?)
                        """, batchId, tenantId, projectId, batchNo, sourceFileId, totalCount, successCount, failCount, status, userId);
        return batchId;
    }

    public void insertImportError(Long tenantId, Long projectId, Long batchId, Long errorId, int rowNo,
                                  String fieldName, String rawValue, String errorCode, String errorMessage) {
        jdbcTemplate.update("""
                        INSERT INTO import_error_detail(error_id, tenant_id, project_id, batch_id, row_no,
                                                        field_name, raw_value, error_code, error_message)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, errorId, tenantId, projectId, batchId, rowNo, fieldName, rawValue, errorCode, errorMessage);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public boolean buildingExists(Long tenantId, Long projectId, Long buildingId) {
        return exists("SELECT COUNT(*) FROM base_building WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND deleted = 0",
                tenantId, projectId, buildingId);
    }

    public boolean unitExists(Long tenantId, Long projectId, Long buildingId, Long unitId) {
        if (unitId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM base_unit WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND unit_id = ? AND deleted = 0",
                tenantId, projectId, buildingId, unitId);
    }

    public boolean houseNoExists(Long tenantId, Long projectId, Long buildingId, Long unitId, String houseNo, Long excludeHouseId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM base_house
                WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND house_no = ? AND deleted = 0
                """);
        args.add(tenantId);
        args.add(projectId);
        args.add(buildingId);
        args.add(houseNo);
        if (unitId == null) {
            sql.append(" AND unit_id IS NULL");
        } else {
            sql.append(" AND unit_id = ?");
            args.add(unitId);
        }
        if (excludeHouseId != null) {
            sql.append(" AND house_id <> ?");
            args.add(excludeHouseId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    private void appendProjectFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (project_code LIKE ? OR project_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND project_id IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private void appendProjectEquals(StringBuilder sql, List<Object> args, Long projectId) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
    }

    private void appendHouseFilters(StringBuilder sql, List<Object> args, Long projectId, Long buildingId,
                                    Long unitId, String keyword) {
        appendProjectEquals(sql, args, projectId);
        if (buildingId != null) {
            sql.append(" AND building_id = ?");
            args.add(buildingId);
        }
        if (unitId != null) {
            sql.append(" AND unit_id = ?");
            args.add(unitId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND house_no LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private ProjectView mapProject(ResultSet rs, int rowNum) throws SQLException {
        return new ProjectView(rs.getLong("project_id"), rs.getString("project_code"), rs.getString("project_name"),
                rs.getString("project_type"), rs.getString("province"), rs.getString("city"), rs.getString("district"),
                rs.getString("address"), (Long) rs.getObject("manager_user_id"), rs.getString("service_phone"),
                rs.getString("collection_subject"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private BuildingView mapBuilding(ResultSet rs, int rowNum) throws SQLException {
        return new BuildingView(rs.getLong("building_id"), rs.getLong("project_id"), rs.getString("building_name"),
                rs.getString("building_code"), rs.getString("building_type"), (Integer) rs.getObject("floor_count"),
                rs.getInt("sort_no"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private UnitView mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitView(rs.getLong("unit_id"), rs.getLong("project_id"), rs.getLong("building_id"),
                rs.getString("unit_name"), rs.getInt("sort_no"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private HouseView mapHouse(ResultSet rs, int rowNum) throws SQLException {
        return new HouseView(rs.getLong("house_id"), rs.getLong("project_id"), rs.getLong("building_id"),
                (Long) rs.getObject("unit_id"), rs.getString("house_no"), (Integer) rs.getObject("floor_no"),
                rs.getBigDecimal("building_area"), rs.getBigDecimal("inner_area"), rs.getString("house_usage"),
                rs.getString("house_status"), rs.getString("charge_object"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
