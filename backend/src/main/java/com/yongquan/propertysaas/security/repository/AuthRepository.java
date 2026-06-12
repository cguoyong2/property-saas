package com.yongquan.propertysaas.security.repository;

import com.yongquan.propertysaas.security.domain.MenuPermission;
import com.yongquan.propertysaas.security.domain.SysUserAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SysUserAccount> findActiveUserByUsername(String username) {
        String sql = """
                SELECT user_id, tenant_id, username, real_name, password_hash, user_type, status
                FROM sys_user
                WHERE username = ? AND deleted = 0
                LIMIT 1
                """;
        List<SysUserAccount> users = jdbcTemplate.query(sql, this::mapUser, username);
        return users.stream().findFirst();
    }

    public Optional<SysUserAccount> findActiveUserById(Long userId) {
        String sql = """
                SELECT user_id, tenant_id, username, real_name, password_hash, user_type, status
                FROM sys_user
                WHERE user_id = ? AND deleted = 0
                LIMIT 1
                """;
        List<SysUserAccount> users = jdbcTemplate.query(sql, this::mapUser, userId);
        return users.stream().findFirst();
    }

    public List<MenuPermission> findMenusByUserId(Long userId) {
        String sql = """
                SELECT DISTINCT m.menu_id, m.parent_id, m.menu_name, m.menu_type, m.permission_code,
                       m.route_path, m.api_path, m.component, m.module_code, m.sort_no, m.visible
                FROM sys_menu m
                JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
                JOIN sys_user_role ur ON ur.role_id = rm.role_id
                WHERE ur.user_id = ?
                  AND m.status = 'ACTIVE'
                  AND m.deleted = 0
                ORDER BY m.sort_no ASC, m.menu_id ASC
                """;
        return jdbcTemplate.query(sql, this::mapMenu, userId);
    }

    public void updateLastLoginAt(Long userId) {
        jdbcTemplate.update("UPDATE sys_user SET last_login_at = NOW() WHERE user_id = ?", userId);
    }

    public void insertLoginLog(Long tenantId, Long userId, String username, String userType,
                               String result, String failReason, String ipAddress, String userAgent) {
        jdbcTemplate.update("""
                        INSERT INTO sys_login_log(login_log_id, tenant_id, user_id, username, user_type,
                                                  login_result, fail_reason, ip_address, user_agent)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000),
                tenantId,
                userId,
                username,
                userType,
                result,
                failReason,
                ipAddress,
                userAgent);
    }

    private SysUserAccount mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new SysUserAccount(
                rs.getLong("user_id"),
                (Long) rs.getObject("tenant_id"),
                rs.getString("username"),
                rs.getString("real_name"),
                rs.getString("password_hash"),
                rs.getString("user_type"),
                rs.getString("status")
        );
    }

    private MenuPermission mapMenu(ResultSet rs, int rowNum) throws SQLException {
        return new MenuPermission(
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
                rs.getBoolean("visible")
        );
    }
}
