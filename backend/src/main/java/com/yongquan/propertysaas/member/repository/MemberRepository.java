package com.yongquan.propertysaas.member.repository;

import com.yongquan.propertysaas.member.domain.MemberHouseBindingView;
import com.yongquan.propertysaas.member.domain.MemberView;
import com.yongquan.propertysaas.member.dto.HouseBindingApplyRequest;
import com.yongquan.propertysaas.member.dto.MemberRequest;
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
                SELECT m.member_id, m.openid, m.unionid, m.mobile, m.real_name, m.avatar_url, m.status,
                       b.project_id, h.building_id, h.unit_id, b.house_id, h.house_no, b.bind_role,
                       m.last_login_at, m.created_at
                FROM member_user m
                LEFT JOIN member_house_bind b
                       ON b.bind_id = (
                         SELECT mb.bind_id
                         FROM member_house_bind mb
                         WHERE mb.tenant_id = m.tenant_id AND mb.member_id = m.member_id
                           AND mb.status = 'APPROVED' AND mb.deleted = 0
                         ORDER BY mb.created_at DESC, mb.bind_id DESC
                         LIMIT 1
                       )
                LEFT JOIN base_house h
                       ON h.tenant_id = m.tenant_id AND h.house_id = b.house_id AND h.deleted = 0
                WHERE m.tenant_id = ? AND m.openid = ? AND m.deleted = 0
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

    public void insertBackofficeMember(Long tenantId, Long memberId, MemberRequest request, String openid) {
        jdbcTemplate.update("""
                        INSERT INTO member_user(member_id, tenant_id, openid, unionid, mobile, real_name,
                                                avatar_url, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                memberId,
                tenantId,
                openid,
                text(request.unionid()),
                text(request.mobile()),
                text(request.realName()),
                text(request.avatarUrl()),
                text(request.status(), "ACTIVE"));
    }

    public void updateBackofficeMember(Long tenantId, Long memberId, MemberRequest request, String openid) {
        jdbcTemplate.update("""
                        UPDATE member_user
                        SET openid = ?,
                            unionid = ?,
                            mobile = ?,
                            real_name = ?,
                            avatar_url = ?,
                            status = ?
                        WHERE tenant_id = ? AND member_id = ? AND deleted = 0
                        """,
                openid,
                text(request.unionid()),
                text(request.mobile()),
                text(request.realName()),
                text(request.avatarUrl()),
                text(request.status(), "ACTIVE"),
                tenantId,
                memberId);
    }

    public void replaceApprovedBinding(Long tenantId, Long memberId, Long bindId, Long userId, MemberRequest request) {
        jdbcTemplate.update("""
                        UPDATE member_house_bind
                        SET status = 'UNBOUND', audit_user_id = ?, audit_at = NOW(), audit_remark = '后台修改房屋绑定'
                        WHERE tenant_id = ? AND member_id = ? AND status = 'APPROVED' AND deleted = 0
                        """, userId, tenantId, memberId);
        jdbcTemplate.update("""
                        INSERT INTO member_house_bind(bind_id, tenant_id, project_id, member_id, house_id, bind_role,
                                                      real_name, mobile, status, effective_date, audit_user_id,
                                                      audit_at, audit_remark, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED', CURRENT_DATE, ?, NOW(), '后台维护绑定', ?)
                        """,
                bindId,
                tenantId,
                request.projectId(),
                memberId,
                request.houseId(),
                text(request.bindRole(), "OWNER"),
                text(request.realName()),
                text(request.mobile()),
                userId,
                userId);
    }

    public void insertBackofficeBindingForAudit(Long tenantId, Long memberId, Long bindId, Long userId, MemberRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO member_house_bind(bind_id, tenant_id, project_id, member_id, house_id, bind_role,
                                                      real_name, mobile, status, effective_date, audit_remark, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_DATE, '物业端提交待审核', ?)
                        """,
                bindId,
                tenantId,
                request.projectId(),
                memberId,
                request.houseId(),
                text(request.bindRole(), "OWNER"),
                text(request.realName()),
                text(request.mobile()),
                userId);
    }

    public List<MemberView> findMembers(Long tenantId, String keyword, Long projectId, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT m.member_id, m.openid, m.unionid, m.mobile, m.real_name, m.avatar_url, m.status,
                       b.project_id, h.building_id, h.unit_id, b.house_id, h.house_no, b.bind_role,
                       m.last_login_at, m.created_at
                FROM member_user m
                LEFT JOIN member_house_bind b
                       ON b.bind_id = (
                         SELECT mb.bind_id
                         FROM member_house_bind mb
                         WHERE mb.tenant_id = m.tenant_id AND mb.member_id = m.member_id
                           AND mb.status = 'APPROVED' AND mb.deleted = 0
                         ORDER BY mb.created_at DESC, mb.bind_id DESC
                         LIMIT 1
                       )
                LEFT JOIN base_house h
                       ON h.tenant_id = m.tenant_id AND h.house_id = b.house_id AND h.deleted = 0
                WHERE m.tenant_id = ? AND m.deleted = 0
                """);
        args.add(tenantId);
        appendMemberFilters(sql, args, keyword, projectId, status, true);
        sql.append(" ORDER BY m.created_at DESC, m.member_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapMember, args.toArray());
    }

    public long countMembers(Long tenantId, String keyword, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM member_user m WHERE m.tenant_id = ? AND m.deleted = 0");
        args.add(tenantId);
        appendMemberFilters(sql, args, keyword, projectId, status, false);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<MemberHouseBindingView> findBindings(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                     Long memberId, String realName, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT b.bind_id, b.project_id, p.project_name, h.building_id, bd.building_name, h.unit_id,
                       u.unit_name, b.member_id, b.house_id, h.house_no,
                       CONCAT(COALESCE(bd.building_name, ''), COALESCE(u.unit_name, ''), COALESCE(h.house_no, '')) AS room_no,
                       b.bind_role, b.real_name, b.mobile,
                       CASE WHEN b.created_by = b.member_id THEN '业主端提交' ELSE '物业端提交' END AS apply_source,
                       b.status, b.effective_date, b.expire_date, b.audit_user_id, b.audit_at,
                       b.audit_remark, b.created_at
                FROM member_house_bind b
                LEFT JOIN base_project p ON p.tenant_id = b.tenant_id AND p.project_id = b.project_id AND p.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = b.tenant_id AND h.house_id = b.house_id AND h.deleted = 0
                LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                WHERE b.tenant_id = ? AND b.deleted = 0
                """);
        args.add(tenantId);
        appendBindingFilters(sql, args, projectId, memberId, realName, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY b.created_at DESC, b.bind_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBinding, args.toArray());
    }

    public long countBindings(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long memberId,
                              String realName, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM member_house_bind b WHERE b.tenant_id = ? AND b.deleted = 0");
        args.add(tenantId);
        appendBindingFilters(sql, args, projectId, memberId, realName, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public MemberHouseBindingView getBinding(Long tenantId, Long bindId) {
        return jdbcTemplate.queryForObject("""
                SELECT b.bind_id, b.project_id, p.project_name, h.building_id, bd.building_name, h.unit_id,
                       u.unit_name, b.member_id, b.house_id, h.house_no,
                       CONCAT(COALESCE(bd.building_name, ''), COALESCE(u.unit_name, ''), COALESCE(h.house_no, '')) AS room_no,
                       b.bind_role, b.real_name, b.mobile,
                       CASE WHEN b.created_by = b.member_id THEN '业主端提交' ELSE '物业端提交' END AS apply_source,
                       b.status, b.effective_date, b.expire_date, b.audit_user_id, b.audit_at,
                       b.audit_remark, b.created_at
                FROM member_house_bind b
                LEFT JOIN base_project p ON p.tenant_id = b.tenant_id AND p.project_id = b.project_id AND p.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = b.tenant_id AND h.house_id = b.house_id AND h.deleted = 0
                LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                WHERE b.tenant_id = ? AND b.bind_id = ? AND b.deleted = 0
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

    public boolean memberRecordExists(Long tenantId, Long memberId) {
        return exists("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND member_id = ? AND deleted = 0",
                tenantId, memberId);
    }

    public boolean mobileExists(Long tenantId, String mobile, Long excludeMemberId) {
        if (mobile == null || mobile.isBlank()) {
            return false;
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM member_user
                WHERE tenant_id = ? AND mobile = ? AND deleted = 0
                """);
        args.add(tenantId);
        args.add(mobile.trim());
        if (excludeMemberId != null) {
            sql.append(" AND member_id <> ?");
            args.add(excludeMemberId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    public boolean houseExists(Long tenantId, Long projectId, Long houseId) {
        return exists("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0",
                tenantId, projectId, houseId);
    }

    public boolean houseHierarchyExists(Long tenantId, Long projectId, Long buildingId, Long unitId, Long houseId) {
        return exists("""
                        SELECT COUNT(*)
                        FROM base_house
                        WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND unit_id = ?
                          AND house_id = ? AND deleted = 0
                        """, tenantId, projectId, buildingId, unitId, houseId);
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

    private void appendMemberFilters(StringBuilder sql, List<Object> args, String keyword, Long projectId,
                                     String status, boolean hasBindingAlias) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (m.real_name LIKE ? OR m.mobile LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (projectId != null) {
            if (hasBindingAlias) {
                sql.append(" AND b.project_id = ?");
            } else {
                sql.append("""
                         AND EXISTS (
                           SELECT 1 FROM member_house_bind mb
                           WHERE mb.tenant_id = m.tenant_id AND mb.member_id = m.member_id
                             AND mb.project_id = ? AND mb.status = 'APPROVED' AND mb.deleted = 0
                         )
                        """);
            }
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND m.status = ?");
            args.add(status);
        }
    }

    private void appendBindingFilters(StringBuilder sql, List<Object> args, Long projectId, Long memberId,
                                      String realName, String status) {
        if (projectId != null) {
            sql.append(" AND b.project_id = ?");
            args.add(projectId);
        }
        if (memberId != null) {
            sql.append(" AND b.member_id = ?");
            args.add(memberId);
        }
        if (realName != null && !realName.isBlank()) {
            sql.append(" AND (b.real_name LIKE ? OR b.mobile LIKE ?)");
            String like = "%" + realName.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND b.status = ?");
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
        sql.append(" AND b.project_id IN (");
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
                (Long) rs.getObject("project_id"),
                (Long) rs.getObject("building_id"),
                (Long) rs.getObject("unit_id"),
                (Long) rs.getObject("house_id"),
                rs.getString("house_no"),
                rs.getString("bind_role"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private MemberHouseBindingView mapBinding(ResultSet rs, int rowNum) throws SQLException {
        return new MemberHouseBindingView(
                rs.getLong("bind_id"),
                rs.getLong("project_id"),
                rs.getString("project_name"),
                (Long) rs.getObject("building_id"),
                rs.getString("building_name"),
                (Long) rs.getObject("unit_id"),
                rs.getString("unit_name"),
                rs.getLong("member_id"),
                rs.getLong("house_id"),
                rs.getString("house_no"),
                rs.getString("room_no"),
                rs.getString("bind_role"),
                rs.getString("real_name"),
                rs.getString("mobile"),
                rs.getString("apply_source"),
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

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String text(String value, String fallback) {
        String text = text(value);
        return text == null ? fallback : text;
    }
}
