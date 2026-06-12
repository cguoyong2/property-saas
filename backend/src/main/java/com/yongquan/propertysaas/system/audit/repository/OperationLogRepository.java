package com.yongquan.propertysaas.system.audit.repository;

import com.yongquan.propertysaas.system.audit.domain.OperationLogView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperationLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long logId, Long tenantId, Long projectId, String operatorType, Long operatorId,
                       String moduleCode, String actionCode, String objectType, Long objectId,
                       String beforeData, String afterData, String reason, String ipAddress, String userAgent) {
        jdbcTemplate.update("""
                        INSERT INTO operation_log(log_id, tenant_id, project_id, operator_type, operator_id,
                                                  module_code, action_code, object_type, object_id,
                                                  before_data, after_data, reason, ip_address, user_agent)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?, ?, ?)
                        """, logId, tenantId, projectId, operatorType, operatorId, moduleCode, actionCode,
                objectType, objectId, beforeData, afterData, reason, ipAddress, userAgent);
    }

    public List<OperationLogView> find(Long tenantId, Long projectId, String moduleCode, String actionCode,
                                       String objectType, Long objectId, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT log_id, tenant_id, project_id, operator_type, operator_id, module_code, action_code,
                       object_type, object_id, CAST(before_data AS CHAR) AS before_data,
                       CAST(after_data AS CHAR) AS after_data, reason, ip_address, user_agent, created_at
                FROM operation_log
                WHERE 1 = 1
                """);
        appendFilters(sql, args, tenantId, projectId, moduleCode, actionCode, objectType, objectId);
        sql.append(" ORDER BY created_at DESC, log_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapLog, args.toArray());
    }

    public long count(Long tenantId, Long projectId, String moduleCode, String actionCode, String objectType, Long objectId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM operation_log WHERE 1 = 1");
        appendFilters(sql, args, tenantId, projectId, moduleCode, actionCode, objectType, objectId);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Long tenantId, Long projectId, String moduleCode,
                               String actionCode, String objectType, Long objectId) {
        if (tenantId != null) {
            sql.append(" AND tenant_id = ?");
            args.add(tenantId);
        }
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (moduleCode != null && !moduleCode.isBlank()) {
            sql.append(" AND module_code = ?");
            args.add(moduleCode.trim());
        }
        if (actionCode != null && !actionCode.isBlank()) {
            sql.append(" AND action_code = ?");
            args.add(actionCode.trim());
        }
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND object_type = ?");
            args.add(objectType.trim());
        }
        if (objectId != null) {
            sql.append(" AND object_id = ?");
            args.add(objectId);
        }
    }

    private OperationLogView mapLog(ResultSet rs, int rowNum) throws SQLException {
        return new OperationLogView(
                rs.getLong("log_id"),
                (Long) rs.getObject("tenant_id"),
                (Long) rs.getObject("project_id"),
                rs.getString("operator_type"),
                (Long) rs.getObject("operator_id"),
                rs.getString("module_code"),
                rs.getString("action_code"),
                rs.getString("object_type"),
                (Long) rs.getObject("object_id"),
                rs.getString("before_data"),
                rs.getString("after_data"),
                rs.getString("reason"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
