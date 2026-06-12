package com.yongquan.propertysaas.security.scope;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectScopeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProjectScopeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        Boolean allTenant = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) > 0
                FROM sys_user_role ur
                JOIN sys_role r ON r.role_id = ur.role_id
                WHERE ur.tenant_id = ? AND ur.user_id = ? AND r.tenant_id = ?
                  AND r.deleted = 0 AND r.status = 'ACTIVE' AND r.data_scope = 'ALL_TENANT'
                """, Boolean.class, tenantId, userId, tenantId);
        if (Boolean.TRUE.equals(allTenant)) {
            return null;
        }
        return jdbcTemplate.queryForList("""
                SELECT project_id
                FROM sys_user_project
                WHERE tenant_id = ? AND user_id = ?
                UNION
                SELECT project_id
                FROM member_house_bind
                WHERE tenant_id = ? AND member_id = ? AND status = 'APPROVED' AND deleted = 0
                ORDER BY project_id ASC
                """, Long.class, tenantId, userId, tenantId, userId);
    }
}
