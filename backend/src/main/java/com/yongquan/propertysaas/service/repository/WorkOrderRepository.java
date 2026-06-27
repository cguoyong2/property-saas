package com.yongquan.propertysaas.service.repository;

import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.service.domain.WorkOrderCommentView;
import com.yongquan.propertysaas.service.domain.WorkOrderEventView;
import com.yongquan.propertysaas.service.domain.WorkOrderView;
import com.yongquan.propertysaas.service.dto.WorkOrderCreateRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public WorkOrderRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<WorkOrderView> findWorkOrders(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                              String status, String orderType, Long handlerUserId,
                                              Long memberId, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT work_order_id, project_id, order_no, member_id, house_id, order_type, title, description,
                       location, image_file_ids, priority, status, accept_user_id, dispatch_user_id,
                       handler_user_id, sla_deadline, completed_at, evaluated_at, created_at
                FROM work_order
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendFilters(sql, args, projectId, status, orderType, handlerUserId, memberId);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, work_order_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapWorkOrder, args.toArray());
    }

    public long countWorkOrders(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                String status, String orderType, Long handlerUserId, Long memberId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM work_order WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendFilters(sql, args, projectId, status, orderType, handlerUserId, memberId);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<WorkOrderView> findWorkOrdersByTypes(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                     String status, List<String> orderTypes, long offset,
                                                     long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT work_order_id, project_id, order_no, member_id, house_id, order_type, title, description,
                       location, image_file_ids, priority, status, accept_user_id, dispatch_user_id,
                       handler_user_id, sla_deadline, completed_at, evaluated_at, created_at
                FROM work_order
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendFilters(sql, args, projectId, status, null, null, null);
        appendOrderTypes(sql, args, orderTypes);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, work_order_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapWorkOrder, args.toArray());
    }

    public long countWorkOrdersByTypes(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                       String status, List<String> orderTypes) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM work_order WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendFilters(sql, args, projectId, status, null, null, null);
        appendOrderTypes(sql, args, orderTypes);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public WorkOrderView getWorkOrder(Long tenantId, Long workOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT work_order_id, project_id, order_no, member_id, house_id, order_type, title, description,
                       location, image_file_ids, priority, status, accept_user_id, dispatch_user_id,
                       handler_user_id, sla_deadline, completed_at, evaluated_at, created_at
                FROM work_order
                WHERE tenant_id = ? AND work_order_id = ? AND deleted = 0
                """, this::mapWorkOrder, tenantId, workOrderId);
    }

    public List<WorkOrderEventView> findEvents(Long tenantId, Long workOrderId) {
        return jdbcTemplate.query("""
                SELECT event_id, work_order_id, from_status, to_status, action, operator_type, operator_id,
                       content, image_file_ids, created_at
                FROM work_order_event
                WHERE tenant_id = ? AND work_order_id = ?
                ORDER BY created_at ASC, event_id ASC
                """, this::mapEvent, tenantId, workOrderId);
    }

    public List<WorkOrderCommentView> findComments(Long tenantId, Long workOrderId) {
        return jdbcTemplate.query("""
                SELECT comment_id, work_order_id, member_id, score, content, created_at
                FROM work_order_comment
                WHERE tenant_id = ? AND work_order_id = ?
                ORDER BY created_at ASC, comment_id ASC
                """, this::mapComment, tenantId, workOrderId);
    }

    public void insertWorkOrder(Long tenantId, Long workOrderId, String orderNo, Long createdBy,
                                WorkOrderCreateRequest request, String priority, LocalDateTime slaDeadline) {
        jdbcTemplate.update("""
                        INSERT INTO work_order(work_order_id, tenant_id, project_id, order_no, member_id, house_id,
                                               order_type, title, description, location, image_file_ids, priority,
                                               status, sla_deadline, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', ?, ?)
                        """, workOrderId, tenantId, request.projectId(), orderNo, request.memberId(), request.houseId(),
                request.orderType(), request.title(), request.description(), request.location(), request.imageFileIds(),
                priority, slaDeadline, createdBy);
    }

    public int updateStatus(Long tenantId, Long workOrderId, String fromStatus, String toStatus, Long userId,
                            Long acceptUserId, Long dispatchUserId, Long handlerUserId, boolean completed,
                            boolean evaluated) {
        return jdbcTemplate.update("""
                UPDATE work_order
                SET status = ?,
                    accept_user_id = COALESCE(?, accept_user_id),
                    dispatch_user_id = COALESCE(?, dispatch_user_id),
                    handler_user_id = COALESCE(?, handler_user_id),
                    completed_at = IF(?, COALESCE(completed_at, NOW()), completed_at),
                    evaluated_at = IF(?, COALESCE(evaluated_at, NOW()), evaluated_at),
                    updated_by = ?
                WHERE tenant_id = ? AND work_order_id = ? AND deleted = 0 AND status = ?
                """, toStatus, acceptUserId, dispatchUserId, handlerUserId, completed, evaluated, userId,
                tenantId, workOrderId, fromStatus);
    }

    public void insertEvent(Long eventId, Long tenantId, Long projectId, Long workOrderId, String fromStatus,
                            String toStatus, String action, String operatorType, Long operatorId,
                            String content, String imageFileIds) {
        jdbcTemplate.update("""
                        INSERT INTO work_order_event(event_id, tenant_id, project_id, work_order_id, from_status,
                                                     to_status, action, operator_type, operator_id, content, image_file_ids)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, eventId, tenantId, projectId, workOrderId, fromStatus, toStatus, action, operatorType,
                operatorId, content, imageFileIds);
    }

    public void insertComment(Long commentId, Long tenantId, Long projectId, Long workOrderId,
                              Long memberId, Integer score, String content) {
        jdbcTemplate.update("""
                        INSERT INTO work_order_comment(comment_id, tenant_id, project_id, work_order_id, member_id,
                                                       score, content)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, commentId, tenantId, projectId, workOrderId, memberId, score, content);
    }

    public List<WorkOrderView> findSlaOverdueCandidates(Long tenantId, List<Long> allowedProjectIds, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT w.work_order_id, w.project_id, w.order_no, w.member_id, w.house_id, w.order_type, w.title,
                       w.description, w.location, w.image_file_ids, w.priority, w.status, w.accept_user_id,
                       w.dispatch_user_id, w.handler_user_id, w.sla_deadline, w.completed_at, w.evaluated_at, w.created_at
                FROM work_order w
                WHERE w.tenant_id = ? AND w.deleted = 0 AND w.sla_deadline < NOW()
                  AND w.status IN ('SUBMITTED','ACCEPTED','DISPATCHED','PROCESSING','HANG_UP','WAIT_CONFIRM','REWORK')
                  AND NOT EXISTS (
                    SELECT 1 FROM work_order_event e
                    WHERE e.tenant_id = w.tenant_id AND e.work_order_id = w.work_order_id
                      AND e.action = 'SLA_OVERDUE'
                  )
                """);
        args.add(tenantId);
        appendProjectScopeForAlias(sql, args, allowedProjectIds, "w.project_id");
        sql.append(" ORDER BY w.sla_deadline ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapWorkOrder, args.toArray());
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

    public List<NoticeRecipient> findProjectUserRecipients(Long tenantId, Long projectId) {
        return jdbcTemplate.query("""
                SELECT 'USER' AS receiver_type, u.user_id AS receiver_id, u.mobile AS receiver_mobile
                FROM sys_user u
                WHERE u.tenant_id = ? AND u.status = 'ACTIVE' AND u.deleted = 0
                  AND (
                    EXISTS (
                      SELECT 1 FROM sys_user_role ur
                      JOIN sys_role r ON r.role_id = ur.role_id
                      WHERE ur.tenant_id = u.tenant_id AND ur.user_id = u.user_id
                        AND r.tenant_id = u.tenant_id AND r.status = 'ACTIVE' AND r.data_scope = 'ALL_TENANT'
                    )
                    OR EXISTS (
                      SELECT 1 FROM sys_user_project p
                      WHERE p.tenant_id = u.tenant_id AND p.user_id = u.user_id AND p.project_id = ?
                    )
                  )
                ORDER BY u.user_id ASC
                """, this::mapRecipient, tenantId, projectId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0",
                tenantId, projectId);
    }

    public boolean houseExists(Long tenantId, Long projectId, Long houseId) {
        return exists("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0",
                tenantId, projectId, houseId);
    }

    public boolean memberExists(Long tenantId, Long memberId) {
        return exists("SELECT COUNT(*) FROM member_user WHERE tenant_id = ? AND member_id = ? AND deleted = 0 AND status = 'ACTIVE'",
                tenantId, memberId);
    }

    public boolean approvedBindingExists(Long tenantId, Long projectId, Long memberId, Long houseId) {
        return exists("""
                SELECT COUNT(*)
                FROM member_house_bind
                WHERE tenant_id = ? AND project_id = ? AND member_id = ? AND house_id = ?
                  AND status = 'APPROVED' AND (bind_role = 'OWNER' OR allow_work_order = 1) AND deleted = 0
                """, tenantId, projectId, memberId, houseId);
    }

    public boolean userExists(Long tenantId, Long userId) {
        return exists("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE' AND deleted = 0",
                tenantId, userId);
    }

    public boolean commentExists(Long tenantId, Long workOrderId, Long memberId) {
        return exists("""
                SELECT COUNT(*)
                FROM work_order_comment
                WHERE tenant_id = ? AND work_order_id = ? AND member_id = ?
                """, tenantId, workOrderId, memberId);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Long projectId, String status, String orderType,
                               Long handlerUserId, Long memberId) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (orderType != null && !orderType.isBlank()) {
            sql.append(" AND order_type = ?");
            args.add(orderType);
        }
        if (handlerUserId != null) {
            sql.append(" AND handler_user_id = ?");
            args.add(handlerUserId);
        }
        if (memberId != null) {
            sql.append(" AND member_id = ?");
            args.add(memberId);
        }
    }

    private void appendOrderTypes(StringBuilder sql, List<Object> args, List<String> orderTypes) {
        if (orderTypes == null || orderTypes.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND order_type IN (");
        sql.append("?,".repeat(orderTypes.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(orderTypes);
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        appendProjectScopeForAlias(sql, args, allowedProjectIds, "project_id");
    }

    private void appendProjectScopeForAlias(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds, String column) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND ").append(column).append(" IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private WorkOrderView mapWorkOrder(ResultSet rs, int rowNum) throws SQLException {
        return new WorkOrderView(rs.getLong("work_order_id"), rs.getLong("project_id"), rs.getString("order_no"),
                (Long) rs.getObject("member_id"), (Long) rs.getObject("house_id"), rs.getString("order_type"),
                rs.getString("title"), rs.getString("description"), rs.getString("location"),
                rs.getString("image_file_ids"), rs.getString("priority"), rs.getString("status"),
                (Long) rs.getObject("accept_user_id"), (Long) rs.getObject("dispatch_user_id"),
                (Long) rs.getObject("handler_user_id"), localDateTime(rs, "sla_deadline"),
                localDateTime(rs, "completed_at"), localDateTime(rs, "evaluated_at"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private WorkOrderEventView mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new WorkOrderEventView(rs.getLong("event_id"), rs.getLong("work_order_id"),
                rs.getString("from_status"), rs.getString("to_status"), rs.getString("action"),
                rs.getString("operator_type"), (Long) rs.getObject("operator_id"), rs.getString("content"),
                rs.getString("image_file_ids"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private WorkOrderCommentView mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new WorkOrderCommentView(rs.getLong("comment_id"), rs.getLong("work_order_id"),
                rs.getLong("member_id"), rs.getInt("score"), rs.getString("content"),
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
