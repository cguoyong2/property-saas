package com.yongquan.propertysaas.system.repository;

import com.yongquan.propertysaas.system.domain.DeptView;
import com.yongquan.propertysaas.system.domain.MenuView;
import com.yongquan.propertysaas.system.domain.RoleView;
import com.yongquan.propertysaas.system.domain.UserView;
import com.yongquan.propertysaas.system.dto.DeptRequest;
import com.yongquan.propertysaas.system.dto.RoleRequest;
import com.yongquan.propertysaas.system.dto.UserRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SystemManagementRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong relationIdSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public SystemManagementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DeptView> findDepts(Long tenantId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT dept_id, parent_id, dept_name, dept_type, project_id, sort_no, status, created_at
                FROM sys_dept
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY parent_id ASC, sort_no ASC, dept_id ASC");
        return jdbcTemplate.query(sql.toString(), this::mapDept, args.toArray());
    }

    public void insertDept(Long tenantId, Long deptId, DeptRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO sys_dept(dept_id, tenant_id, parent_id, dept_name, dept_type, project_id, sort_no, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                deptId,
                tenantId,
                value(request.parentId(), 0L),
                request.deptName(),
                text(request.deptType(), "TENANT"),
                request.projectId(),
                value(request.sortNo(), 0),
                text(request.status(), "ACTIVE"));
    }

    public void updateDept(Long tenantId, Long deptId, DeptRequest request) {
        jdbcTemplate.update("""
                        UPDATE sys_dept
                        SET parent_id = ?, dept_name = ?, dept_type = ?, project_id = ?, sort_no = ?, status = ?
                        WHERE tenant_id = ? AND dept_id = ? AND deleted = 0
                        """,
                value(request.parentId(), 0L),
                request.deptName(),
                text(request.deptType(), "TENANT"),
                request.projectId(),
                value(request.sortNo(), 0),
                text(request.status(), "ACTIVE"),
                tenantId,
                deptId);
    }

    public List<UserView> findUsers(Long tenantId, String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT user_id, dept_id, username, real_name, mobile, user_type, status, last_login_at, created_at
                FROM sys_user
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendUserFilters(sql, args, keyword, status);
        sql.append(" ORDER BY created_at DESC, user_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapUser, args.toArray()).stream()
                .map(user -> new UserView(
                        user.userId(),
                        user.deptId(),
                        user.username(),
                        user.realName(),
                        user.mobile(),
                        user.userType(),
                        user.status(),
                        user.lastLoginAt(),
                        user.createdAt(),
                        findRoleIdsByUser(tenantId, user.userId()),
                        findProjectIdsByUser(tenantId, user.userId())))
                .toList();
    }

    public long countUsers(Long tenantId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendUserFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count, 0L);
    }

    public UserView getUser(Long tenantId, Long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT user_id, dept_id, username, real_name, mobile, user_type, status, last_login_at, created_at
                FROM sys_user
                WHERE tenant_id = ? AND user_id = ? AND user_type = 'TENANT' AND deleted = 0
                """, this::mapUser, tenantId, userId);
    }

    public void insertUser(Long tenantId, Long userId, UserRequest request, String passwordHash) {
        jdbcTemplate.update("""
                        INSERT INTO sys_user(user_id, tenant_id, dept_id, username, real_name, mobile,
                                             password_hash, user_type, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'TENANT', ?)
                        """,
                userId,
                tenantId,
                request.deptId(),
                request.username(),
                request.realName(),
                request.mobile(),
                passwordHash,
                text(request.status(), "ACTIVE"));
    }

    public void updateUser(Long tenantId, Long userId, UserRequest request) {
        jdbcTemplate.update("""
                        UPDATE sys_user
                        SET dept_id = ?, real_name = ?, mobile = ?, status = ?
                        WHERE tenant_id = ? AND user_id = ? AND user_type = 'TENANT' AND deleted = 0
                        """,
                request.deptId(),
                request.realName(),
                request.mobile(),
                text(request.status(), "ACTIVE"),
                tenantId,
                userId);
    }

    public void resetPassword(Long tenantId, Long userId, String passwordHash) {
        jdbcTemplate.update("""
                        UPDATE sys_user
                        SET password_hash = ?
                        WHERE tenant_id = ? AND user_id = ? AND user_type = 'TENANT' AND deleted = 0
                        """, passwordHash, tenantId, userId);
    }

    public void updateUserStatus(Long tenantId, Long userId, String status) {
        jdbcTemplate.update("""
                        UPDATE sys_user
                        SET status = ?
                        WHERE tenant_id = ? AND user_id = ? AND user_type = 'TENANT' AND deleted = 0
                        """, status, tenantId, userId);
    }

    public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE tenant_id = ? AND user_id = ?", tenantId, userId);
        for (Long roleId : roleIds) {
            jdbcTemplate.update("""
                            INSERT INTO sys_user_role(id, tenant_id, user_id, role_id)
                            VALUES (?, ?, ?, ?)
                            """, relationId(), tenantId, userId, roleId);
        }
    }

    public void replaceUserProjects(Long tenantId, Long userId, List<Long> projectIds) {
        jdbcTemplate.update("DELETE FROM sys_user_project WHERE tenant_id = ? AND user_id = ?", tenantId, userId);
        for (Long projectId : projectIds) {
            jdbcTemplate.update("""
                            INSERT INTO sys_user_project(id, tenant_id, user_id, project_id)
                            VALUES (?, ?, ?, ?)
                            """, relationId(), tenantId, userId, projectId);
        }
    }

    public List<RoleView> findRoles(Long tenantId, String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT role_id, role_name, role_code, role_level, data_scope, status, created_at
                FROM sys_role
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendRoleFilters(sql, args, keyword, status);
        sql.append(" ORDER BY created_at DESC, role_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapRole, args.toArray());
    }

    public long countRoles(Long tenantId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_role WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendRoleFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count, 0L);
    }

    public void insertRole(Long tenantId, Long roleId, String roleCode, RoleRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO sys_role(role_id, tenant_id, role_name, role_code, role_level, data_scope, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                roleId,
                tenantId,
                request.roleName(),
                roleCode,
                text(request.roleLevel(), "TENANT"),
                text(request.dataScope(), "SELF"),
                text(request.status(), "ACTIVE"));
    }

    public void updateRole(Long tenantId, Long roleId, String roleCode, RoleRequest request) {
        jdbcTemplate.update("""
                        UPDATE sys_role
                        SET role_name = ?, role_code = ?, role_level = ?, data_scope = ?, status = ?
                        WHERE tenant_id = ? AND role_id = ? AND deleted = 0
                        """,
                request.roleName(),
                roleCode,
                text(request.roleLevel(), "TENANT"),
                text(request.dataScope(), "SELF"),
                text(request.status(), "ACTIVE"),
                tenantId,
                roleId);
    }

    public void replaceRoleMenus(Long tenantId, Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE tenant_id = ? AND role_id = ?", tenantId, roleId);
        for (Long menuId : menuIds) {
            jdbcTemplate.update("""
                            INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id)
                            VALUES (?, ?, ?, ?)
                            """, relationId(), tenantId, roleId, menuId);
        }
    }

    public List<MenuView> findMenus(String moduleCode) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path,
                       component, module_code, sort_no, visible, status
                FROM sys_menu
                WHERE deleted = 0
                """);
        if (moduleCode != null && !moduleCode.isBlank()) {
            sql.append(" AND module_code = ?");
            args.add(moduleCode);
        }
        sql.append(" ORDER BY parent_id ASC, sort_no ASC, menu_id ASC");
        return jdbcTemplate.query(sql.toString(), this::mapMenu, args.toArray());
    }

    public List<Long> findRoleMenuIds(Long tenantId, Long roleId) {
        return jdbcTemplate.queryForList("""
                SELECT menu_id
                FROM sys_role_menu
                WHERE tenant_id = ? AND role_id = ?
                ORDER BY menu_id ASC
                """, Long.class, tenantId, roleId);
    }

    public boolean deptExists(Long tenantId, Long deptId) {
        if (deptId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM sys_dept WHERE tenant_id = ? AND dept_id = ? AND deleted = 0", tenantId, deptId);
    }

    public boolean roleExists(Long tenantId, Long roleId) {
        return exists("SELECT COUNT(*) FROM sys_role WHERE tenant_id = ? AND role_id = ? AND deleted = 0", tenantId, roleId);
    }

    public boolean userExists(Long tenantId, Long userId) {
        return exists("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND user_id = ? AND user_type = 'TENANT' AND deleted = 0", tenantId, userId);
    }

    public boolean menuExists(Long menuId) {
        return exists("SELECT COUNT(*) FROM sys_menu WHERE menu_id = ? AND deleted = 0", menuId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public List<Long> findRoleIdsByUser(Long tenantId, Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT role_id
                FROM sys_user_role
                WHERE tenant_id = ? AND user_id = ?
                ORDER BY role_id ASC
                """, Long.class, tenantId, userId);
    }

    public List<Long> findProjectIdsByUser(Long tenantId, Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT project_id
                FROM sys_user_project
                WHERE tenant_id = ? AND user_id = ?
                ORDER BY project_id ASC
                """, Long.class, tenantId, userId);
    }

    private void appendUserFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (username LIKE ? OR real_name LIKE ? OR mobile LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private void appendRoleFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (role_name LIKE ? OR role_code LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private DeptView mapDept(ResultSet rs, int rowNum) throws SQLException {
        return new DeptView(
                rs.getLong("dept_id"),
                rs.getLong("parent_id"),
                rs.getString("dept_name"),
                rs.getString("dept_type"),
                (Long) rs.getObject("project_id"),
                rs.getInt("sort_no"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private UserView mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserView(
                rs.getLong("user_id"),
                (Long) rs.getObject("dept_id"),
                rs.getString("username"),
                rs.getString("real_name"),
                rs.getString("mobile"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime(),
                List.of(),
                List.of()
        );
    }

    private RoleView mapRole(ResultSet rs, int rowNum) throws SQLException {
        return new RoleView(
                rs.getLong("role_id"),
                rs.getString("role_name"),
                rs.getString("role_code"),
                rs.getString("role_level"),
                rs.getString("data_scope"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private MenuView mapMenu(ResultSet rs, int rowNum) throws SQLException {
        return new MenuView(
                rs.getLong("menu_id"),
                rs.getLong("parent_id"),
                rs.getString("menu_name"),
                rs.getString("menu_type"),
                rs.getString("permission_code"),
                rs.getString("route_path"),
                rs.getString("api_path"),
                rs.getString("component"),
                rs.getString("module_code"),
                rs.getInt("sort_no"),
                rs.getBoolean("visible"),
                rs.getString("status")
        );
    }

    private Long relationId() {
        return relationIdSequence.incrementAndGet();
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Long value(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer value(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }
}
