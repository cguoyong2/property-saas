package com.yongquan.propertysaas.file.repository;

import com.yongquan.propertysaas.file.domain.FileObjectView;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FileObjectRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public FileObjectRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public void insert(FileObjectView file) {
        jdbcTemplate.update("""
                        INSERT INTO file_object(file_id, tenant_id, project_id, module_code, original_name,
                                                object_key, file_ext, content_type, file_size, is_sensitive,
                                                uploader_type, uploader_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, file.fileId(), file.tenantId(), file.projectId(), file.moduleCode(),
                file.originalName(), file.objectKey(), file.fileExt(), file.contentType(), file.fileSize(),
                Boolean.TRUE.equals(file.sensitive()), file.uploaderType(), file.uploaderId());
    }

    public FileObjectView get(Long tenantId, Long fileId) {
        return jdbcTemplate.queryForObject("""
                SELECT file_id, tenant_id, project_id, module_code, original_name, object_key, file_ext,
                       content_type, file_size, is_sensitive, uploader_type, uploader_id, created_at
                FROM file_object
                WHERE tenant_id = ? AND file_id = ? AND deleted = 0
                """, this::mapFile, tenantId, fileId);
    }

    public List<FileObjectView> find(Long tenantId, List<Long> allowedProjectIds, Long projectId, String moduleCode,
                                     long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT file_id, tenant_id, project_id, module_code, original_name, object_key, file_ext,
                       content_type, file_size, is_sensitive, uploader_type, uploader_id, created_at
                FROM file_object
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendFilters(sql, args, projectId, moduleCode);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, file_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapFile, args.toArray());
    }

    public long count(Long tenantId, List<Long> allowedProjectIds, Long projectId, String moduleCode) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM file_object WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendFilters(sql, args, projectId, moduleCode);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public int delete(Long tenantId, Long fileId) {
        return jdbcTemplate.update("""
                UPDATE file_object
                SET deleted = 1
                WHERE tenant_id = ? AND file_id = ? AND deleted = 0
                """, tenantId, fileId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM base_project
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0
                """, Integer.class, tenantId, projectId);
        return count != null && count > 0;
    }

    public long countValidFiles(Long tenantId, Long projectId, String moduleCode, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM file_object
                WHERE tenant_id = ? AND project_id = ? AND module_code = ? AND deleted = 0
                  AND file_id IN (
                """);
        sql.append("?,".repeat(fileIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(projectId);
        args.add(moduleCode);
        args.addAll(fileIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Long projectId, String moduleCode) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (moduleCode != null && !moduleCode.isBlank()) {
            sql.append(" AND module_code = ?");
            args.add(moduleCode);
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

    private FileObjectView mapFile(ResultSet rs, int rowNum) throws SQLException {
        return new FileObjectView(rs.getLong("file_id"), (Long) rs.getObject("tenant_id"),
                (Long) rs.getObject("project_id"), rs.getString("module_code"), rs.getString("original_name"),
                rs.getString("object_key"), rs.getString("file_ext"), rs.getString("content_type"),
                rs.getLong("file_size"), rs.getBoolean("is_sensitive"), rs.getString("uploader_type"),
                (Long) rs.getObject("uploader_id"), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
