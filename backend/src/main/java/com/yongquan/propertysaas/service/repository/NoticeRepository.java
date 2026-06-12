package com.yongquan.propertysaas.service.repository;

import com.yongquan.propertysaas.service.domain.MessageRecordView;
import com.yongquan.propertysaas.service.domain.MessageTemplateView;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.service.domain.NoticeView;
import com.yongquan.propertysaas.service.dto.MessageTemplateRequest;
import com.yongquan.propertysaas.service.dto.NoticeCreateRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class NoticeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public NoticeRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<NoticeView> findPlatformNotices(Long tenantId, Long projectId, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT notice_id, tenant_id, project_id, title, content, notice_type, target_scope,
                       publish_status, published_at, publisher_id, created_at
                FROM notice
                WHERE deleted = 0
                """);
        appendNoticeFilters(sql, args, tenantId, projectId, status);
        sql.append(" ORDER BY created_at DESC, notice_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapNotice, args.toArray());
    }

    public long countPlatformNotices(Long tenantId, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM notice WHERE deleted = 0");
        appendNoticeFilters(sql, args, tenantId, projectId, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<NoticeView> findTenantNotices(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                              String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT notice_id, tenant_id, project_id, title, content, notice_type, target_scope,
                       publish_status, published_at, publisher_id, created_at
                FROM notice
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendNoticeFilters(sql, args, null, projectId, status);
        appendNoticeProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, notice_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapNotice, args.toArray());
    }

    public long countTenantNotices(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM notice WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendNoticeFilters(sql, args, null, projectId, status);
        appendNoticeProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<NoticeView> findAppNotices(Long tenantId, Long projectId, Long memberId, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT n.notice_id, n.tenant_id, n.project_id, n.title, n.content, n.notice_type,
                       n.target_scope, n.publish_status, n.published_at, n.publisher_id, n.created_at
                FROM notice n
                LEFT JOIN message_record m ON m.tenant_id = n.tenant_id
                  AND m.project_id <=> n.project_id
                  AND m.title = n.title
                  AND m.content = n.content
                  AND m.receiver_type = 'MEMBER'
                  AND m.receiver_id = ?
                WHERE n.tenant_id = ? AND n.deleted = 0 AND n.publish_status = 'PUBLISHED'
                  AND (n.target_scope = 'ALL_TENANT' OR n.project_id = ? OR m.message_id IS NOT NULL)
                ORDER BY n.published_at DESC, n.notice_id DESC
                LIMIT ? OFFSET ?
                """);
        args.add(memberId);
        args.add(tenantId);
        args.add(projectId);
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapNotice, args.toArray());
    }

    public long countAppNotices(Long tenantId, Long projectId, Long memberId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT n.notice_id)
                FROM notice n
                LEFT JOIN message_record m ON m.tenant_id = n.tenant_id
                  AND m.project_id <=> n.project_id
                  AND m.title = n.title
                  AND m.content = n.content
                  AND m.receiver_type = 'MEMBER'
                  AND m.receiver_id = ?
                WHERE n.tenant_id = ? AND n.deleted = 0 AND n.publish_status = 'PUBLISHED'
                  AND (n.target_scope = 'ALL_TENANT' OR n.project_id = ? OR m.message_id IS NOT NULL)
                """, Long.class, memberId, tenantId, projectId);
        return value(count);
    }

    public NoticeView getNotice(Long tenantId, Long noticeId) {
        return jdbcTemplate.queryForObject("""
                SELECT notice_id, tenant_id, project_id, title, content, notice_type, target_scope,
                       publish_status, published_at, publisher_id, created_at
                FROM notice
                WHERE tenant_id = ? AND notice_id = ? AND deleted = 0
                """, this::mapNotice, tenantId, noticeId);
    }

    public void insertNotice(Long tenantId, Long noticeId, Long userId, NoticeCreateRequest request,
                             String noticeType, String targetScope, String publishStatus) {
        jdbcTemplate.update("""
                        INSERT INTO notice(notice_id, tenant_id, project_id, title, content, notice_type,
                                           target_scope, publish_status, published_at, publisher_id, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, IF(? = 'PUBLISHED', NOW(), NULL), IF(? = 'PUBLISHED', ?, NULL), ?)
                        """,
                noticeId, tenantId, request.projectId(), request.title(), request.content(), noticeType,
                targetScope, publishStatus, publishStatus, publishStatus, userId, userId);
    }

    public int publishNotice(Long tenantId, Long noticeId, Long userId) {
        return jdbcTemplate.update("""
                UPDATE notice
                SET publish_status = 'PUBLISHED', published_at = COALESCE(published_at, NOW()),
                    publisher_id = COALESCE(publisher_id, ?), updated_by = ?
                WHERE tenant_id = ? AND notice_id = ? AND deleted = 0 AND publish_status = 'DRAFT'
                """, userId, userId, tenantId, noticeId);
    }

    public int withdrawNotice(Long tenantId, Long noticeId, Long userId) {
        return jdbcTemplate.update("""
                UPDATE notice
                SET publish_status = 'WITHDRAWN', updated_by = ?
                WHERE tenant_id = ? AND notice_id = ? AND deleted = 0 AND publish_status = 'PUBLISHED'
                """, userId, tenantId, noticeId);
    }

    public List<MessageRecordView> findMessages(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                String channel, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT message_id, tenant_id, project_id, receiver_type, receiver_id, receiver_mobile,
                       channel, template_code, title, content, send_status, fail_reason, sent_at, created_at
                FROM message_record
                WHERE tenant_id = ?
                """);
        args.add(tenantId);
        appendMessageFilters(sql, args, projectId, channel, status);
        appendMessageProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, message_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapMessage, args.toArray());
    }

    public long countMessages(Long tenantId, List<Long> allowedProjectIds, Long projectId, String channel, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM message_record WHERE tenant_id = ?");
        args.add(tenantId);
        appendMessageFilters(sql, args, projectId, channel, status);
        appendMessageProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public void insertMessage(Long messageId, Long tenantId, Long projectId, NoticeRecipient recipient,
                              String channel, String templateCode, String title, String content) {
        jdbcTemplate.update("""
                        INSERT INTO message_record(message_id, tenant_id, project_id, receiver_type, receiver_id,
                                                   receiver_mobile, channel, template_code, title, content, send_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, messageId, tenantId, projectId, recipient.receiverType(), recipient.receiverId(),
                recipient.receiverMobile(), channel, templateCode, title, content);
    }

    public List<MessageTemplateView> findMessageTemplates(Long tenantId, String channel, String status,
                                                          long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT template_id, tenant_id, template_code, template_name, channel, title_template,
                       content_template, status, created_at
                FROM message_template
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendTemplateFilters(sql, args, channel, status);
        sql.append(" ORDER BY created_at DESC, template_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapTemplate, args.toArray());
    }

    public long countMessageTemplates(Long tenantId, String channel, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM message_template WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendTemplateFilters(sql, args, channel, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public MessageTemplateView getMessageTemplate(Long tenantId, Long templateId) {
        return jdbcTemplate.queryForObject("""
                SELECT template_id, tenant_id, template_code, template_name, channel, title_template,
                       content_template, status, created_at
                FROM message_template
                WHERE tenant_id = ? AND template_id = ? AND deleted = 0
                """, this::mapTemplate, tenantId, templateId);
    }

    public boolean messageTemplateCodeExists(Long tenantId, Long templateId, String templateCode, String channel) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM message_template
                WHERE tenant_id = ? AND template_code = ? AND channel = ? AND deleted = 0
                  AND (? IS NULL OR template_id <> ?)
                """, Long.class, tenantId, templateCode, channel, templateId, templateId);
        return value(count) > 0;
    }

    public void insertMessageTemplate(Long tenantId, Long templateId, Long userId, MessageTemplateRequest request,
                                      String channel, String status) {
        jdbcTemplate.update("""
                        INSERT INTO message_template(template_id, tenant_id, template_code, template_name, channel,
                                                     title_template, content_template, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, templateId, tenantId, request.templateCode(), request.templateName(), channel,
                request.titleTemplate(), request.contentTemplate(), status, userId);
    }

    public int updateMessageTemplate(Long tenantId, Long templateId, Long userId, MessageTemplateRequest request,
                                     String channel, String status) {
        return jdbcTemplate.update("""
                UPDATE message_template
                SET template_code = ?, template_name = ?, channel = ?, title_template = ?, content_template = ?,
                    status = ?, updated_by = ?
                WHERE tenant_id = ? AND template_id = ? AND deleted = 0
                """, request.templateCode(), request.templateName(), channel, request.titleTemplate(),
                request.contentTemplate(), status, userId, tenantId, templateId);
    }

    public MessageRecordView getMessage(Long tenantId, Long messageId) {
        return jdbcTemplate.queryForObject("""
                SELECT message_id, tenant_id, project_id, receiver_type, receiver_id, receiver_mobile,
                       channel, template_code, title, content, send_status, fail_reason, sent_at, created_at
                FROM message_record
                WHERE tenant_id = ? AND message_id = ?
                """, this::mapMessage, tenantId, messageId);
    }

    public List<MessageRecordView> findDispatchCandidates(Long tenantId, List<Long> allowedProjectIds,
                                                          String status, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT message_id, tenant_id, project_id, receiver_type, receiver_id, receiver_mobile,
                       channel, template_code, title, content, send_status, fail_reason, sent_at, created_at
                FROM message_record
                WHERE tenant_id = ? AND send_status = ?
                """);
        args.add(tenantId);
        args.add(status);
        appendMessageProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at ASC, message_id ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapMessage, args.toArray());
    }

    public int markMessageSent(Long tenantId, Long messageId) {
        return jdbcTemplate.update("""
                UPDATE message_record
                SET send_status = 'SENT', sent_at = NOW(), fail_reason = NULL
                WHERE tenant_id = ? AND message_id = ? AND send_status IN ('PENDING', 'FAILED')
                """, tenantId, messageId);
    }

    public int markMessageFailed(Long tenantId, Long messageId, String failReason) {
        return jdbcTemplate.update("""
                UPDATE message_record
                SET send_status = 'FAILED', fail_reason = ?, sent_at = NULL
                WHERE tenant_id = ? AND message_id = ? AND send_status IN ('PENDING', 'FAILED')
                """, failReason, tenantId, messageId);
    }

    public List<NoticeRecipient> findTenantAdminRecipients(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT 'USER' AS receiver_type, user_id AS receiver_id, mobile AS receiver_mobile
                FROM sys_user
                WHERE tenant_id = ? AND user_type = 'TENANT' AND status = 'ACTIVE' AND deleted = 0
                ORDER BY user_id ASC
                """, this::mapRecipient, tenantId);
    }

    public List<NoticeRecipient> findProjectMemberRecipients(Long tenantId, Long projectId) {
        return jdbcTemplate.query("""
                SELECT 'MEMBER' AS receiver_type, b.member_id AS receiver_id, b.mobile AS receiver_mobile
                FROM member_house_bind b
                JOIN member_user m ON m.tenant_id = b.tenant_id AND m.member_id = b.member_id AND m.deleted = 0
                WHERE b.tenant_id = ? AND b.project_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                GROUP BY b.member_id, b.mobile
                ORDER BY b.member_id ASC
                """, this::mapRecipient, tenantId, projectId);
    }

    public List<NoticeRecipient> findTenantMemberRecipients(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT 'MEMBER' AS receiver_type, member_id AS receiver_id, mobile AS receiver_mobile
                FROM member_user
                WHERE tenant_id = ? AND status = 'ACTIVE' AND deleted = 0
                ORDER BY member_id ASC
                """, this::mapRecipient, tenantId);
    }

    public List<NoticeRecipient> findMemberRecipients(Long tenantId, Long projectId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT 'MEMBER' AS receiver_type, m.member_id AS receiver_id, m.mobile AS receiver_mobile
                FROM member_user m
                WHERE m.tenant_id = ? AND m.status = 'ACTIVE' AND m.deleted = 0
                """);
        args.add(tenantId);
        appendIn(sql, args, "m.member_id", memberIds);
        if (projectId != null) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1 FROM member_house_bind b
                       WHERE b.tenant_id = m.tenant_id AND b.member_id = m.member_id
                         AND b.project_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                     )
                    """);
            args.add(projectId);
        }
        return jdbcTemplate.query(sql.toString(), this::mapRecipient, args.toArray());
    }

    public List<NoticeRecipient> findHouseMemberRecipients(Long tenantId, Long projectId, List<Long> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT 'MEMBER' AS receiver_type, b.member_id AS receiver_id, b.mobile AS receiver_mobile
                FROM member_house_bind b
                WHERE b.tenant_id = ? AND b.project_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                """);
        args.add(tenantId);
        args.add(projectId);
        appendIn(sql, args, "b.house_id", houseIds);
        sql.append(" GROUP BY b.member_id, b.mobile ORDER BY b.member_id ASC");
        return jdbcTemplate.query(sql.toString(), this::mapRecipient, args.toArray());
    }

    public boolean tenantExists(Long tenantId) {
        return exists("SELECT COUNT(*) FROM sys_tenant WHERE tenant_id = ? AND deleted = 0", tenantId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0",
                tenantId, projectId);
    }

    private void appendNoticeFilters(StringBuilder sql, List<Object> args, Long tenantId, Long projectId, String status) {
        if (tenantId != null) {
            sql.append(" AND tenant_id = ?");
            args.add(tenantId);
        }
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND publish_status = ?");
            args.add(status);
        }
    }

    private void appendNoticeProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
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

    private void appendMessageFilters(StringBuilder sql, List<Object> args, Long projectId, String channel, String status) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = ?");
            args.add(channel);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND send_status = ?");
            args.add(status);
        }
    }

    private void appendTemplateFilters(StringBuilder sql, List<Object> args, String channel, String status) {
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = ?");
            args.add(channel);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private void appendMessageProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
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

    private void appendIn(StringBuilder sql, List<Object> args, String column, List<Long> values) {
        sql.append(" AND ").append(column).append(" IN (");
        sql.append("?,".repeat(values.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(values);
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private NoticeView mapNotice(ResultSet rs, int rowNum) throws SQLException {
        return new NoticeView(rs.getLong("notice_id"), rs.getLong("tenant_id"), (Long) rs.getObject("project_id"),
                rs.getString("title"), rs.getString("content"), rs.getString("notice_type"),
                rs.getString("target_scope"), rs.getString("publish_status"), localDateTime(rs, "published_at"),
                (Long) rs.getObject("publisher_id"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private MessageRecordView mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new MessageRecordView(rs.getLong("message_id"), (Long) rs.getObject("tenant_id"),
                (Long) rs.getObject("project_id"), rs.getString("receiver_type"), (Long) rs.getObject("receiver_id"),
                rs.getString("receiver_mobile"), rs.getString("channel"), rs.getString("template_code"),
                rs.getString("title"), rs.getString("content"), rs.getString("send_status"),
                rs.getString("fail_reason"), localDateTime(rs, "sent_at"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private MessageTemplateView mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new MessageTemplateView(rs.getLong("template_id"), rs.getLong("tenant_id"),
                rs.getString("template_code"), rs.getString("template_name"), rs.getString("channel"),
                rs.getString("title_template"), rs.getString("content_template"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private NoticeRecipient mapRecipient(ResultSet rs, int rowNum) throws SQLException {
        return new NoticeRecipient(rs.getString("receiver_type"), (Long) rs.getObject("receiver_id"),
                rs.getString("receiver_mobile"));
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
