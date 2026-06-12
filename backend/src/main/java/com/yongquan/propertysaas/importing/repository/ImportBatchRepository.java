package com.yongquan.propertysaas.importing.repository;

import com.yongquan.propertysaas.importing.domain.ImportBatchView;
import com.yongquan.propertysaas.importing.domain.ImportErrorDetailView;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImportBatchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public ImportBatchRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<ImportBatchView> findBatches(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                             String importType, String importStatus, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT batch_id, tenant_id, project_id, import_type, batch_no, source_file_id, total_count,
                       success_count, fail_count, import_status, error_report_file_id, can_rollback,
                       rollback_status, created_by, created_at, updated_at
                FROM import_batch
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendFilters(sql, args, projectId, importType, importStatus);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, batch_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBatch, args.toArray());
    }

    public long countBatches(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                             String importType, String importStatus) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM import_batch WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendFilters(sql, args, projectId, importType, importStatus);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    public ImportBatchView getBatch(Long tenantId, Long batchId) {
        return jdbcTemplate.queryForObject("""
                SELECT batch_id, tenant_id, project_id, import_type, batch_no, source_file_id, total_count,
                       success_count, fail_count, import_status, error_report_file_id, can_rollback,
                       rollback_status, created_by, created_at, updated_at
                FROM import_batch
                WHERE tenant_id = ? AND batch_id = ? AND deleted = 0
                """, this::mapBatch, tenantId, batchId);
    }

    public List<ImportErrorDetailView> findErrors(Long tenantId, Long batchId, long offset, long pageSize) {
        return jdbcTemplate.query("""
                SELECT error_id, batch_id, row_no, field_name, raw_value, error_code, error_message, created_at
                FROM import_error_detail
                WHERE tenant_id = ? AND batch_id = ?
                ORDER BY row_no ASC, error_id ASC LIMIT ? OFFSET ?
                """, this::mapError, tenantId, batchId, pageSize, offset);
    }

    public long countErrors(Long tenantId, Long batchId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM import_error_detail
                WHERE tenant_id = ? AND batch_id = ?
                """, Long.class, tenantId, batchId);
        return count == null ? 0L : count;
    }

    public List<ImportErrorDetailView> findAllErrors(Long tenantId, Long batchId) {
        return jdbcTemplate.query("""
                SELECT error_id, batch_id, row_no, field_name, raw_value, error_code, error_message, created_at
                FROM import_error_detail
                WHERE tenant_id = ? AND batch_id = ?
                ORDER BY row_no ASC, error_id ASC
                """, this::mapError, tenantId, batchId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM base_project
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0
                """, Integer.class, tenantId, projectId);
        return count != null && count > 0;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Long projectId, String importType, String importStatus) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (importType != null && !importType.isBlank()) {
            sql.append(" AND import_type = ?");
            args.add(importType);
        }
        if (importStatus != null && !importStatus.isBlank()) {
            sql.append(" AND import_status = ?");
            args.add(importStatus);
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
        sql.append(" AND (project_id IS NULL OR project_id IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append("))");
        args.addAll(allowedProjectIds);
    }

    private ImportBatchView mapBatch(ResultSet rs, int rowNum) throws SQLException {
        return new ImportBatchView(rs.getLong("batch_id"), rs.getLong("tenant_id"), (Long) rs.getObject("project_id"),
                rs.getString("import_type"), rs.getString("batch_no"), (Long) rs.getObject("source_file_id"),
                rs.getInt("total_count"), rs.getInt("success_count"), rs.getInt("fail_count"),
                rs.getString("import_status"), (Long) rs.getObject("error_report_file_id"),
                rs.getBoolean("can_rollback"), rs.getString("rollback_status"), (Long) rs.getObject("created_by"),
                rs.getTimestamp("created_at").toLocalDateTime(), localDateTime(rs, "updated_at"));
    }

    private ImportErrorDetailView mapError(ResultSet rs, int rowNum) throws SQLException {
        return new ImportErrorDetailView(rs.getLong("error_id"), rs.getLong("batch_id"), rs.getInt("row_no"),
                rs.getString("field_name"), rs.getString("raw_value"), rs.getString("error_code"),
                rs.getString("error_message"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }
}
