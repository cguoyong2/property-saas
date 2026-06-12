package com.yongquan.propertysaas.vehicle.repository;

import com.yongquan.propertysaas.vehicle.domain.ParkingSpaceView;
import com.yongquan.propertysaas.vehicle.domain.ParkingSyncRecordView;
import com.yongquan.propertysaas.vehicle.domain.VehicleView;
import com.yongquan.propertysaas.vehicle.dto.MonthlyRentRequest;
import com.yongquan.propertysaas.vehicle.dto.ParkingSpaceRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public VehicleRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<ParkingSpaceView> findSpaces(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                             String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT space_id, project_id, space_no, space_type, status, house_id, created_at
                FROM base_parking_space
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendKeyword(sql, args, "space_no", keyword);
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY project_id ASC, space_no ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSpace, args.toArray());
    }

    public long countSpaces(Long tenantId, List<Long> allowedProjectIds, Long projectId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_parking_space WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendKeyword(sql, args, "space_no", keyword);
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public ParkingSpaceView getSpace(Long tenantId, Long spaceId) {
        return jdbcTemplate.queryForObject("""
                SELECT space_id, project_id, space_no, space_type, status, house_id, created_at
                FROM base_parking_space
                WHERE tenant_id = ? AND space_id = ? AND deleted = 0
                """, this::mapSpace, tenantId, spaceId);
    }

    public void insertSpace(Long tenantId, Long spaceId, Long userId, ParkingSpaceRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_parking_space(space_id, tenant_id, project_id, space_no, space_type,
                                                       status, house_id, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                spaceId, tenantId, request.projectId(), request.spaceNo(), text(request.spaceType(), "UNDERGROUND"),
                text(request.status(), "AVAILABLE"), request.houseId(), userId);
    }

    public void updateSpace(Long tenantId, Long spaceId, Long userId, ParkingSpaceRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_parking_space
                        SET project_id = ?, space_no = ?, space_type = ?, status = ?, house_id = ?, updated_by = ?
                        WHERE tenant_id = ? AND space_id = ? AND deleted = 0
                        """,
                request.projectId(), request.spaceNo(), text(request.spaceType(), "UNDERGROUND"),
                text(request.status(), "AVAILABLE"), request.houseId(), userId, tenantId, spaceId);
    }

    public List<VehicleView> findVehicles(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                          String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT vehicle_id, project_id, plate_no, vehicle_type, member_id, house_id, space_id,
                       monthly_rent_status, start_date, end_date, status, created_at
                FROM base_vehicle
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendKeyword(sql, args, "plate_no", keyword);
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY project_id ASC, plate_no ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapVehicle, args.toArray());
    }

    public long countVehicles(Long tenantId, List<Long> allowedProjectIds, Long projectId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_vehicle WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendKeyword(sql, args, "plate_no", keyword);
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public VehicleView getVehicle(Long tenantId, Long vehicleId) {
        return jdbcTemplate.queryForObject("""
                SELECT vehicle_id, project_id, plate_no, vehicle_type, member_id, house_id, space_id,
                       monthly_rent_status, start_date, end_date, status, created_at
                FROM base_vehicle
                WHERE tenant_id = ? AND vehicle_id = ? AND deleted = 0
                """, this::mapVehicle, tenantId, vehicleId);
    }

    public void insertVehicle(Long tenantId, Long vehicleId, Long userId, VehicleRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_vehicle(vehicle_id, tenant_id, project_id, plate_no, vehicle_type, member_id,
                                                 house_id, space_id, monthly_rent_status, start_date, end_date,
                                                 status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                vehicleId, tenantId, request.projectId(), normalizePlate(request.plateNo()), text(request.vehicleType(), "CAR"),
                request.memberId(), request.houseId(), request.spaceId(), text(request.monthlyRentStatus(), "NONE"),
                request.startDate(), request.endDate(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateVehicle(Long tenantId, Long vehicleId, Long userId, VehicleRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_vehicle
                        SET project_id = ?, plate_no = ?, vehicle_type = ?, member_id = ?, house_id = ?, space_id = ?,
                            monthly_rent_status = ?, start_date = ?, end_date = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND vehicle_id = ? AND deleted = 0
                        """,
                request.projectId(), normalizePlate(request.plateNo()), text(request.vehicleType(), "CAR"), request.memberId(),
                request.houseId(), request.spaceId(), text(request.monthlyRentStatus(), "NONE"), request.startDate(),
                request.endDate(), text(request.status(), "ACTIVE"), userId, tenantId, vehicleId);
    }

    public void updateMonthlyRent(Long tenantId, Long vehicleId, Long userId, MonthlyRentRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_vehicle
                        SET monthly_rent_status = ?, start_date = ?, end_date = ?, updated_by = ?
                        WHERE tenant_id = ? AND vehicle_id = ? AND deleted = 0
                        """, request.monthlyRentStatus(), request.startDate(), request.endDate(), userId, tenantId, vehicleId);
    }

    public void insertParkingSyncRecord(Long syncId, Long tenantId, Long projectId, String vendorCode, String plateNo,
                                        String syncType, String requestData) {
        jdbcTemplate.update("""
                        INSERT INTO parking_sync_record(sync_id, tenant_id, project_id, vendor_code, plate_no,
                                                        sync_type, request_data, sync_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, syncId, tenantId, projectId, vendorCode, normalizePlate(plateNo), syncType, requestData);
    }

    public List<ParkingSyncRecordView> findParkingSyncRecords(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                              String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT sync_id, project_id, vendor_code, plate_no, sync_type, sync_status,
                       error_message, retry_count, created_at
                FROM parking_sync_record
                WHERE tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND sync_status = ?");
            args.add(status);
        }
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, sync_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSync, args.toArray());
    }

    public long countParkingSyncRecords(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM parking_sync_record WHERE tenant_id = ?");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND sync_status = ?");
            args.add(status);
        }
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public boolean houseExists(Long tenantId, Long projectId, Long houseId) {
        if (houseId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0",
                tenantId, projectId, houseId);
    }

    public boolean memberExists(Long tenantId, Long memberId) {
        if (memberId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND member_id = ? AND deleted = 0", tenantId, memberId);
    }

    public boolean spaceExists(Long tenantId, Long projectId, Long spaceId) {
        if (spaceId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM base_parking_space WHERE tenant_id = ? AND project_id = ? AND space_id = ? AND deleted = 0",
                tenantId, projectId, spaceId);
    }

    public boolean plateExists(Long tenantId, Long projectId, String plateNo, Long excludeVehicleId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM base_vehicle
                WHERE tenant_id = ? AND project_id = ? AND plate_no = ? AND deleted = 0
                """);
        args.add(tenantId);
        args.add(projectId);
        args.add(normalizePlate(plateNo));
        if (excludeVehicleId != null) {
            sql.append(" AND vehicle_id <> ?");
            args.add(excludeVehicleId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
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

    private void appendKeyword(StringBuilder sql, List<Object> args, String field, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND ").append(field).append(" LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String status) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private ParkingSpaceView mapSpace(ResultSet rs, int rowNum) throws SQLException {
        return new ParkingSpaceView(rs.getLong("space_id"), rs.getLong("project_id"), rs.getString("space_no"),
                rs.getString("space_type"), rs.getString("status"), (Long) rs.getObject("house_id"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private VehicleView mapVehicle(ResultSet rs, int rowNum) throws SQLException {
        return new VehicleView(rs.getLong("vehicle_id"), rs.getLong("project_id"), rs.getString("plate_no"),
                rs.getString("vehicle_type"), (Long) rs.getObject("member_id"), (Long) rs.getObject("house_id"),
                (Long) rs.getObject("space_id"), rs.getString("monthly_rent_status"),
                rs.getObject("start_date", java.time.LocalDate.class), rs.getObject("end_date", java.time.LocalDate.class),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private ParkingSyncRecordView mapSync(ResultSet rs, int rowNum) throws SQLException {
        return new ParkingSyncRecordView(rs.getLong("sync_id"), rs.getLong("project_id"), rs.getString("vendor_code"),
                rs.getString("plate_no"), rs.getString("sync_type"), rs.getString("sync_status"),
                rs.getString("error_message"), rs.getInt("retry_count"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalizePlate(String plateNo) {
        return plateNo == null ? null : plateNo.trim().toUpperCase();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
