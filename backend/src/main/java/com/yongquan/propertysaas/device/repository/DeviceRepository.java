package com.yongquan.propertysaas.device.repository;

import com.yongquan.propertysaas.device.domain.AccessPermissionView;
import com.yongquan.propertysaas.device.domain.AccessRecordView;
import com.yongquan.propertysaas.device.domain.DeviceConfigView;
import com.yongquan.propertysaas.device.domain.VisitorRecordView;
import com.yongquan.propertysaas.device.dto.AccessPermissionRequest;
import com.yongquan.propertysaas.device.dto.AccessRecordRequest;
import com.yongquan.propertysaas.device.dto.DeviceConfigRequest;
import com.yongquan.propertysaas.device.dto.VisitorInviteRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public DeviceRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<DeviceConfigView> findDevices(Long tenantId, List<Long> scope, Long projectId, String deviceType,
                                              String vendorCode, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT device_id, project_id, device_type, vendor_code, device_code, device_name,
                       location, CAST(config_json AS CHAR) AS config_json, status, created_at
                FROM device_config
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendDeviceFilters(sql, args, projectId, deviceType, vendorCode, status);
        appendProjectScope(sql, args, scope);
        sql.append(" ORDER BY created_at DESC, device_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapDevice, args.toArray());
    }

    public long countDevices(Long tenantId, List<Long> scope, Long projectId, String deviceType, String vendorCode, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM device_config WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendDeviceFilters(sql, args, projectId, deviceType, vendorCode, status);
        appendProjectScope(sql, args, scope);
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public DeviceConfigView getDevice(Long tenantId, Long deviceId) {
        return jdbcTemplate.queryForObject("""
                SELECT device_id, project_id, device_type, vendor_code, device_code, device_name,
                       location, CAST(config_json AS CHAR) AS config_json, status, created_at
                FROM device_config
                WHERE tenant_id = ? AND device_id = ? AND deleted = 0
                """, this::mapDevice, tenantId, deviceId);
    }

    public void insertDevice(Long tenantId, Long deviceId, Long userId, DeviceConfigRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO device_config(device_id, tenant_id, project_id, device_type, vendor_code, device_code,
                                                  device_name, location, config_json, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, deviceId, tenantId, request.projectId(), request.deviceType(), request.vendorCode(),
                request.deviceCode(), request.deviceName(), request.location(), request.configJson(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateDevice(Long tenantId, Long deviceId, Long userId, DeviceConfigRequest request) {
        jdbcTemplate.update("""
                        UPDATE device_config
                        SET project_id = ?, device_type = ?, vendor_code = ?, device_code = ?, device_name = ?,
                            location = ?, config_json = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND device_id = ? AND deleted = 0
                        """, request.projectId(), request.deviceType(), request.vendorCode(), request.deviceCode(),
                request.deviceName(), request.location(), request.configJson(), text(request.status(), "ACTIVE"), userId, tenantId, deviceId);
    }

    public List<VisitorRecordView> findVisitors(Long tenantId, List<Long> scope, Long projectId, String status,
                                                long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT v.visitor_id, v.project_id, p.project_name, v.inviter_member_id, m.real_name AS inviter_member_name,
                       v.house_id, CONCAT(COALESCE(b.building_name, ''), COALESCE(u.unit_name, ''), COALESCE(h.house_no, '')) AS house_no,
                       v.visitor_name, v.visitor_mobile, v.visit_reason, v.valid_start_at, v.valid_end_at,
                       v.qr_code, v.status, v.created_at
                FROM visitor_record v
                LEFT JOIN base_project p ON p.tenant_id = v.tenant_id AND p.project_id = v.project_id AND p.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = v.tenant_id AND m.member_id = v.inviter_member_id AND m.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = v.tenant_id AND h.house_id = v.house_id AND h.deleted = 0
                LEFT JOIN base_building b ON b.tenant_id = h.tenant_id AND b.building_id = h.building_id AND b.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                WHERE v.tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId, "v.project_id");
        appendStatus(sql, args, status, "v.status");
        appendProjectScope(sql, args, scope, "v.project_id");
        sql.append(" ORDER BY v.created_at DESC, v.visitor_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapVisitor, args.toArray());
    }

    public long countVisitors(Long tenantId, List<Long> scope, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM visitor_record WHERE tenant_id = ?");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, scope);
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public VisitorRecordView getVisitor(Long tenantId, Long visitorId) {
        return jdbcTemplate.queryForObject("""
                SELECT v.visitor_id, v.project_id, p.project_name, v.inviter_member_id, m.real_name AS inviter_member_name,
                       v.house_id, CONCAT(COALESCE(b.building_name, ''), COALESCE(u.unit_name, ''), COALESCE(h.house_no, '')) AS house_no,
                       v.visitor_name, v.visitor_mobile, v.visit_reason, v.valid_start_at, v.valid_end_at,
                       v.qr_code, v.status, v.created_at
                FROM visitor_record v
                LEFT JOIN base_project p ON p.tenant_id = v.tenant_id AND p.project_id = v.project_id AND p.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = v.tenant_id AND m.member_id = v.inviter_member_id AND m.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = v.tenant_id AND h.house_id = v.house_id AND h.deleted = 0
                LEFT JOIN base_building b ON b.tenant_id = h.tenant_id AND b.building_id = h.building_id AND b.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                WHERE v.tenant_id = ? AND v.visitor_id = ?
                """, this::mapVisitor, tenantId, visitorId);
    }

    public void insertVisitor(Long tenantId, Long visitorId, VisitorInviteRequest request, String qrCode) {
        jdbcTemplate.update("""
                        INSERT INTO visitor_record(visitor_id, tenant_id, project_id, inviter_member_id, house_id, visitor_name,
                                                   visitor_mobile, visit_reason, valid_start_at, valid_end_at, qr_code, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED')
                        """, visitorId, tenantId, request.projectId(), request.inviterMemberId(), request.houseId(), request.visitorName(),
                request.visitorMobile(), request.visitReason(), request.validStartAt(), request.validEndAt(), qrCode);
    }

    public List<AccessPermissionView> findAccessPermissions(Long tenantId, List<Long> scope, Long projectId, Long deviceId,
                                                            String status, String syncStatus, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT p.permission_id, p.project_id, p.member_id, p.user_id, p.visitor_id, p.device_id,
                       d.device_name, d.vendor_code, d.device_code, p.permission_type, p.start_at, p.end_at,
                       p.status, p.sync_status, p.created_at
                FROM access_permission p
                JOIN device_config d ON d.tenant_id = p.tenant_id AND d.device_id = p.device_id AND d.deleted = 0
                WHERE p.tenant_id = ?
                """);
        args.add(tenantId);
        appendPermissionFilters(sql, args, projectId, deviceId, status, syncStatus, "p.project_id", "p.device_id");
        appendProjectScope(sql, args, scope, "p.project_id");
        sql.append(" ORDER BY p.created_at DESC, p.permission_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapPermission, args.toArray());
    }

    public long countAccessPermissions(Long tenantId, List<Long> scope, Long projectId, Long deviceId, String status, String syncStatus) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM access_permission WHERE tenant_id = ?");
        args.add(tenantId);
        appendPermissionFilters(sql, args, projectId, deviceId, status, syncStatus, "project_id", "device_id");
        appendProjectScope(sql, args, scope);
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public AccessPermissionView getPermission(Long tenantId, Long permissionId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.permission_id, p.project_id, p.member_id, p.user_id, p.visitor_id, p.device_id,
                       d.device_name, d.vendor_code, d.device_code, p.permission_type, p.start_at, p.end_at,
                       p.status, p.sync_status, p.created_at
                FROM access_permission p
                JOIN device_config d ON d.tenant_id = p.tenant_id AND d.device_id = p.device_id AND d.deleted = 0
                WHERE p.tenant_id = ? AND p.permission_id = ?
                """, this::mapPermission, tenantId, permissionId);
    }

    public void insertAccessPermission(Long tenantId, Long permissionId, AccessPermissionRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO access_permission(permission_id, tenant_id, project_id, member_id, user_id, visitor_id,
                                                      device_id, permission_type, start_at, end_at, status, sync_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, permissionId, tenantId, request.projectId(), request.memberId(), request.userId(), request.visitorId(),
                request.deviceId(), request.permissionType(), request.startAt(), request.endAt(), text(request.status(), "ACTIVE"));
    }

    public List<AccessPermissionView> findPendingPermissions(Long tenantId, List<Long> scope, Long projectId, Long deviceId, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT p.permission_id, p.project_id, p.member_id, p.user_id, p.visitor_id, p.device_id,
                       d.device_name, d.vendor_code, d.device_code, p.permission_type, p.start_at, p.end_at,
                       p.status, p.sync_status, p.created_at
                FROM access_permission p
                JOIN device_config d ON d.tenant_id = p.tenant_id AND d.device_id = p.device_id AND d.deleted = 0
                WHERE p.tenant_id = ? AND p.sync_status IN ('PENDING', 'FAILED') AND p.status = 'ACTIVE'
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId, "p.project_id");
        if (deviceId != null) {
            sql.append(" AND p.device_id = ?");
            args.add(deviceId);
        }
        appendProjectScope(sql, args, scope, "p.project_id");
        sql.append(" ORDER BY p.created_at ASC, p.permission_id ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapPermission, args.toArray());
    }

    public void updatePermissionSyncStatus(Long tenantId, Long permissionId, String syncStatus) {
        jdbcTemplate.update("""
                UPDATE access_permission
                SET sync_status = ?
                WHERE tenant_id = ? AND permission_id = ?
                """, syncStatus, tenantId, permissionId);
    }

    public void insertAccessRecord(Long recordId, Long tenantId, AccessRecordRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO access_record(record_id, tenant_id, project_id, device_id, member_id, visitor_id,
                                                  open_type, open_result, occurred_at, raw_data)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, recordId, tenantId, request.projectId(), request.deviceId(), request.memberId(), request.visitorId(),
                request.openType(), request.openResult(), request.occurredAt(), request.rawData());
    }

    public List<AccessRecordView> findAccessRecords(Long tenantId, List<Long> scope, Long projectId, Long deviceId,
                                                    long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT record_id, project_id, device_id, member_id, visitor_id, open_type, open_result,
                       occurred_at, CAST(raw_data AS CHAR) AS raw_data
                FROM access_record
                WHERE tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (deviceId != null) {
            sql.append(" AND device_id = ?");
            args.add(deviceId);
        }
        appendProjectScope(sql, args, scope);
        sql.append(" ORDER BY occurred_at DESC, record_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapRecord, args.toArray());
    }

    public long countAccessRecords(Long tenantId, List<Long> scope, Long projectId, Long deviceId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM access_record WHERE tenant_id = ?");
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        if (deviceId != null) {
            sql.append(" AND device_id = ?");
            args.add(deviceId);
        }
        appendProjectScope(sql, args, scope);
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public void insertInterfaceLog(Long logId, Long tenantId, Long projectId, String interfaceType, String vendorCode,
                                   String requestId, String requestUrl, String requestBody, String responseBody,
                                   boolean success, String errorMessage, Integer costMs) {
        jdbcTemplate.update("""
                        INSERT INTO interface_call_log(log_id, tenant_id, project_id, interface_type, vendor_code,
                                                       request_id, request_url, request_body, response_body, success,
                                                       error_message, cost_ms)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, logId, tenantId, projectId, interfaceType, vendorCode, requestId, requestUrl, requestBody,
                responseBody, success ? 1 : 0, errorMessage, costMs);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public boolean memberExists(Long tenantId, Long memberId) {
        if (memberId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND member_id = ? AND deleted = 0", tenantId, memberId);
    }

    public boolean houseExists(Long tenantId, Long projectId, Long houseId) {
        if (houseId == null) {
            return true;
        }
        return exists("""
                SELECT COUNT(*) FROM base_house
                WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0
                """, tenantId, projectId, houseId);
    }

    public boolean memberHouseBindingExists(Long tenantId, Long projectId, Long memberId, Long houseId) {
        if (memberId == null || houseId == null) {
            return true;
        }
        return exists("""
                SELECT COUNT(*) FROM member_house_bind
                WHERE tenant_id = ? AND project_id = ? AND member_id = ? AND house_id = ?
                  AND status = 'APPROVED' AND deleted = 0
                """, tenantId, projectId, memberId, houseId);
    }

    public boolean userExists(Long tenantId, Long userId) {
        if (userId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND user_id = ? AND deleted = 0", tenantId, userId);
    }

    public boolean deviceExists(Long tenantId, Long projectId, Long deviceId) {
        return exists("SELECT COUNT(*) FROM device_config WHERE tenant_id = ? AND project_id = ? AND device_id = ? AND deleted = 0",
                tenantId, projectId, deviceId);
    }

    public boolean visitorExists(Long tenantId, Long projectId, Long visitorId) {
        if (visitorId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM visitor_record WHERE tenant_id = ? AND project_id = ? AND visitor_id = ?",
                tenantId, projectId, visitorId);
    }

    private void appendDeviceFilters(StringBuilder sql, List<Object> args, Long projectId, String deviceType,
                                     String vendorCode, String status) {
        appendProjectEquals(sql, args, projectId);
        if (deviceType != null && !deviceType.isBlank()) {
            sql.append(" AND device_type = ?");
            args.add(deviceType);
        }
        if (vendorCode != null && !vendorCode.isBlank()) {
            sql.append(" AND vendor_code = ?");
            args.add(vendorCode);
        }
        appendStatus(sql, args, status);
    }

    private void appendPermissionFilters(StringBuilder sql, List<Object> args, Long projectId, Long deviceId,
                                         String status, String syncStatus, String projectField, String deviceField) {
        appendProjectEquals(sql, args, projectId, projectField);
        if (deviceId != null) {
            sql.append(" AND ").append(deviceField).append(" = ?");
            args.add(deviceId);
        }
        appendStatus(sql, args, status);
        if (syncStatus != null && !syncStatus.isBlank()) {
            sql.append(" AND sync_status = ?");
            args.add(syncStatus);
        }
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds, String field) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND ").append(field).append(" IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private void appendProjectEquals(StringBuilder sql, List<Object> args, Long projectId) {
        appendProjectEquals(sql, args, projectId, "project_id");
    }

    private void appendProjectEquals(StringBuilder sql, List<Object> args, Long projectId, String field) {
        if (projectId != null) {
            sql.append(" AND ").append(field).append(" = ?");
            args.add(projectId);
        }
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String status) {
        appendStatus(sql, args, status, "status");
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String status, String field) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND ").append(field).append(" = ?");
            args.add(status);
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private DeviceConfigView mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new DeviceConfigView(rs.getLong("device_id"), rs.getLong("project_id"), rs.getString("device_type"),
                rs.getString("vendor_code"), rs.getString("device_code"), rs.getString("device_name"),
                rs.getString("location"), rs.getString("config_json"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private VisitorRecordView mapVisitor(ResultSet rs, int rowNum) throws SQLException {
        return new VisitorRecordView(rs.getLong("visitor_id"), rs.getLong("project_id"), rs.getString("project_name"),
                (Long) rs.getObject("inviter_member_id"), rs.getString("inviter_member_name"),
                (Long) rs.getObject("house_id"), rs.getString("house_no"),
                rs.getString("visitor_name"), rs.getString("visitor_mobile"), rs.getString("visit_reason"),
                rs.getTimestamp("valid_start_at").toLocalDateTime(), rs.getTimestamp("valid_end_at").toLocalDateTime(),
                rs.getString("qr_code"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private AccessPermissionView mapPermission(ResultSet rs, int rowNum) throws SQLException {
        return new AccessPermissionView(rs.getLong("permission_id"), rs.getLong("project_id"), (Long) rs.getObject("member_id"),
                (Long) rs.getObject("user_id"), (Long) rs.getObject("visitor_id"), rs.getLong("device_id"),
                rs.getString("device_name"), rs.getString("vendor_code"), rs.getString("device_code"),
                rs.getString("permission_type"), rs.getTimestamp("start_at").toLocalDateTime(),
                rs.getTimestamp("end_at") == null ? null : rs.getTimestamp("end_at").toLocalDateTime(),
                rs.getString("status"), rs.getString("sync_status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private AccessRecordView mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new AccessRecordView(rs.getLong("record_id"), rs.getLong("project_id"), (Long) rs.getObject("device_id"),
                (Long) rs.getObject("member_id"), (Long) rs.getObject("visitor_id"), rs.getString("open_type"),
                rs.getString("open_result"), rs.getTimestamp("occurred_at").toLocalDateTime(), rs.getString("raw_data"));
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
