package com.yongquan.propertysaas.member.repository;

import com.yongquan.propertysaas.member.domain.MemberHouseBindingView;
import com.yongquan.propertysaas.member.domain.MemberView;
import com.yongquan.propertysaas.member.dto.HouseBindingApplyRequest;
import com.yongquan.propertysaas.member.dto.WxLoginRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public MemberRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public Optional<MemberView> findMemberByOpenid(Long tenantId, String openid) {
        List<MemberView> members = jdbcTemplate.query("""
                SELECT member_id, openid, unionid, mobile, real_name, avatar_url, status, last_login_at, created_at
                FROM member_user
                WHERE tenant_id = ? AND openid = ? AND deleted = 0
                LIMIT 1
                """, this::mapMember, tenantId, openid);
        return members.stream().findFirst();
    }

    public void insertMember(Long tenantId, Long memberId, WxLoginRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO member_user(member_id, tenant_id, openid, unionid, mobile, real_name,
                                                avatar_url, status, last_login_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())
                        """,
                memberId,
                tenantId,
                request.openid(),
                request.unionid(),
                request.mobile(),
                request.realName(),
                request.avatarUrl());
    }

    public void updateMemberLogin(Long tenantId, Long memberId, WxLoginRequest request) {
        jdbcTemplate.update("""
                        UPDATE member_user
                        SET unionid = COALESCE(?, unionid),
                            mobile = COALESCE(?, mobile),
                            real_name = COALESCE(?, real_name),
                            avatar_url = COALESCE(?, avatar_url),
                            last_login_at = NOW()
                        WHERE tenant_id = ? AND member_id = ? AND deleted = 0
                        """,
                request.unionid(),
                request.mobile(),
                request.realName(),
                request.avatarUrl(),
                tenantId,
                memberId);
    }

    public List<MemberView> findMembers(Long tenantId, String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT member_id, openid, unionid, mobile, real_name, avatar_url, status, last_login_at, created_at
                FROM member_user
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendMemberFilters(sql, args, keyword, status);
        sql.append(" ORDER BY created_at DESC, member_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapMember, args.toArray());
    }

    public long countMembers(Long tenantId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendMemberFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<MemberHouseBindingView> findBindings(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                     Long memberId, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT bind_id, project_id, member_id, house_id, bind_role, real_name, mobile, status,
                       effective_date, expire_date, audit_user_id, audit_at, audit_remark, created_at
                FROM member_house_bind
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendBindingFilters(sql, args, projectId, memberId, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, bind_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBinding, args.toArray());
    }

    public long countBindings(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long memberId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM member_house_bind WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendBindingFilters(sql, args, projectId, memberId, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public MemberHouseBindingView getBinding(Long tenantId, Long bindId) {
        return jdbcTemplate.queryForObject("""
                SELECT bind_id, project_id, member_id, house_id, bind_role, real_name, mobile, status,
                       effective_date, expire_date, audit_user_id, audit_at, audit_remark, created_at
                FROM member_house_bind
                WHERE tenant_id = ? AND bind_id = ? AND deleted = 0
                """, this::mapBinding, tenantId, bindId);
    }

    public void insertBinding(Long bindId, HouseBindingApplyRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO member_house_bind(bind_id, tenant_id, project_id, member_id, house_id, bind_role,
                                                      real_name, mobile, id_card_no_encrypted, proof_file_ids,
                                                      status, effective_date, expire_date, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?)
                        """,
                bindId,
                request.tenantId(),
                request.projectId(),
                request.memberId(),
                request.houseId(),
                request.bindRole(),
                request.realName(),
                request.mobile(),
                request.idCardNoEncrypted(),
                request.proofFileIds(),
                request.effectiveDate(),
                request.expireDate(),
                request.memberId());
    }

    public void auditBinding(Long tenantId, Long bindId, String status, Long auditUserId, String auditRemark) {
        jdbcTemplate.update("""
                        UPDATE member_house_bind
                        SET status = ?, audit_user_id = ?, audit_at = NOW(), audit_remark = ?
                        WHERE tenant_id = ? AND bind_id = ? AND deleted = 0
                        """, status, auditUserId, auditRemark, tenantId, bindId);
    }

    public void unbind(Long tenantId, Long bindId, Long userId, String reason) {
        jdbcTemplate.update("""
                        UPDATE member_house_bind
                        SET status = 'UNBOUND', audit_user_id = ?, audit_at = NOW(), audit_remark = ?
                        WHERE tenant_id = ? AND bind_id = ? AND deleted = 0
                        """, userId, reason, tenantId, bindId);
    }

    public boolean tenantExists(Long tenantId) {
        return exists("SELECT COUNT(*) FROM sys_tenant WHERE tenant_id = ? AND deleted = 0 AND status IN ('TRIAL','ACTIVE')", tenantId);
    }

    public boolean memberExists(Long tenantId, Long memberId) {
        return exists("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND member_id = ? AND deleted = 0 AND status = 'ACTIVE'",
                tenantId, memberId);
    }

    public boolean houseExists(Long tenantId, Long projectId, Long houseId) {
        return exists("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0",
                tenantId, projectId, houseId);
    }

    public boolean approvedBindingExists(Long tenantId, Long memberId, Long houseId) {
        return exists("""
                SELECT COUNT(*) FROM member_house_bind
                WHERE tenant_id = ? AND member_id = ? AND house_id = ?
                  AND status = 'APPROVED' AND deleted = 0
                """, tenantId, memberId, houseId);
    }

    public boolean pendingBindingExists(Long tenantId, Long memberId, Long houseId) {
        return exists("""
                SELECT COUNT(*) FROM member_house_bind
                WHERE tenant_id = ? AND member_id = ? AND house_id = ?
                  AND status = 'PENDING' AND deleted = 0
                """, tenantId, memberId, houseId);
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    private void appendMemberFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (real_name LIKE ? OR mobile LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private void appendBindingFilters(StringBuilder sql, List<Object> args, Long projectId, Long memberId, String status) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (memberId != null) {
            sql.append(" AND member_id = ?");
            args.add(memberId);
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

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private MemberView mapMember(ResultSet rs, int rowNum) throws SQLException {
        return new MemberView(
                rs.getLong("member_id"),
                rs.getString("openid"),
                rs.getString("unionid"),
                rs.getString("mobile"),
                rs.getString("real_name"),
                rs.getString("avatar_url"),
                rs.getString("status"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private MemberHouseBindingView mapBinding(ResultSet rs, int rowNum) throws SQLException {
        return new MemberHouseBindingView(
                rs.getLong("bind_id"),
                rs.getLong("project_id"),
                rs.getLong("member_id"),
                rs.getLong("house_id"),
                rs.getString("bind_role"),
                rs.getString("real_name"),
                rs.getString("mobile"),
                rs.getString("status"),
                rs.getObject("effective_date", java.time.LocalDate.class),
                rs.getObject("expire_date", java.time.LocalDate.class),
                (Long) rs.getObject("audit_user_id"),
                rs.getTimestamp("audit_at") == null ? null : rs.getTimestamp("audit_at").toLocalDateTime(),
                rs.getString("audit_remark"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
