package com.yongquan.propertysaas.job.repository;

import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findRunnableTenantIds() {
        return jdbcTemplate.queryForList("""
                SELECT tenant_id
                FROM sys_tenant
                WHERE deleted = 0 AND status IN ('TRIAL', 'ACTIVE', 'ARREARS_LIMITED')
                ORDER BY tenant_id ASC
                """, Long.class);
    }

    public List<WorkOrderSlaCandidate> findSlaOverdueCandidates(Long tenantId, int limit) {
        return jdbcTemplate.query("""
                SELECT w.work_order_id, w.project_id, w.order_no, w.status, w.title, w.sla_deadline
                FROM work_order w
                WHERE w.tenant_id = ? AND w.deleted = 0 AND w.sla_deadline < NOW()
                  AND w.status IN ('SUBMITTED','ACCEPTED','DISPATCHED','PROCESSING','HANG_UP','WAIT_CONFIRM','REWORK')
                  AND NOT EXISTS (
                    SELECT 1 FROM work_order_event e
                    WHERE e.tenant_id = w.tenant_id AND e.work_order_id = w.work_order_id
                      AND e.action = 'SLA_OVERDUE'
                  )
                ORDER BY w.sla_deadline ASC
                LIMIT ?
                """, (rs, rowNum) -> new WorkOrderSlaCandidate(
                rs.getLong("work_order_id"),
                rs.getLong("project_id"),
                rs.getString("order_no"),
                rs.getString("status"),
                rs.getString("title"),
                rs.getTimestamp("sla_deadline").toLocalDateTime()
        ), tenantId, limit);
    }

    public void insertWorkOrderEvent(Long eventId, Long tenantId, Long projectId, Long workOrderId, String fromStatus,
                                     String action, String content) {
        jdbcTemplate.update("""
                        INSERT INTO work_order_event(event_id, tenant_id, project_id, work_order_id, from_status,
                                                     to_status, action, operator_type, operator_id, content, image_file_ids)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'SYSTEM', NULL, ?, NULL)
                        """, eventId, tenantId, projectId, workOrderId, fromStatus, fromStatus, action, content);
    }

    public int markPatrolTaskItemsMissed(Long tenantId, Long taskId) {
        return jdbcTemplate.update("""
                UPDATE patrol_task_item
                SET result = 'MISSED', status = 'MISSED'
                WHERE tenant_id = ? AND task_id = ? AND status = 'PENDING'
                """, tenantId, taskId);
    }

    public List<PatrolMissedCandidate> findPatrolMissedCandidates(Long tenantId, int limit) {
        return jdbcTemplate.query("""
                SELECT task_id, project_id, task_no, task_name, planned_end_at
                FROM patrol_task
                WHERE tenant_id = ? AND deleted = 0 AND status = 'PENDING' AND planned_end_at < NOW()
                ORDER BY planned_end_at ASC
                LIMIT ?
                """, (rs, rowNum) -> new PatrolMissedCandidate(
                rs.getLong("task_id"),
                rs.getLong("project_id"),
                rs.getString("task_no"),
                rs.getString("task_name"),
                rs.getTimestamp("planned_end_at").toLocalDateTime()
        ), tenantId, limit);
    }

    public int markPatrolTaskMissed(Long tenantId, Long taskId) {
        return jdbcTemplate.update("""
                UPDATE patrol_task
                SET status = 'MISSED', actual_end_at = COALESCE(actual_end_at, NOW())
                WHERE tenant_id = ? AND task_id = ? AND deleted = 0 AND status = 'PENDING'
                """, tenantId, taskId);
    }

    public List<LeaseExpireCandidate> findLeaseExpireCandidates(Long tenantId, int days, int limit) {
        LocalDate endDate = LocalDate.now().plusDays(Math.max(days, 0));
        return jdbcTemplate.query("""
                SELECT c.contract_id, c.project_id, c.contract_no, c.end_date
                FROM lease_contract c
                WHERE c.tenant_id = ? AND c.deleted = 0 AND c.status = 'ACTIVE'
                  AND c.end_date BETWEEN CURDATE() AND ?
                  AND NOT EXISTS (
                    SELECT 1 FROM message_record m
                    WHERE m.tenant_id = c.tenant_id AND m.project_id = c.project_id
                      AND m.template_code = 'LEASE_CONTRACT_EXPIRE'
                      AND m.content = CONCAT('合同 ', c.contract_no, ' 将于 ', c.end_date, ' 到期')
                  )
                ORDER BY c.end_date ASC, c.contract_id ASC
                LIMIT ?
                """, (rs, rowNum) -> new LeaseExpireCandidate(
                rs.getLong("contract_id"),
                rs.getLong("project_id"),
                rs.getString("contract_no"),
                rs.getDate("end_date").toLocalDate()
        ), tenantId, endDate, limit);
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
                """, (rs, rowNum) -> new NoticeRecipient(
                rs.getString("receiver_type"),
                rs.getLong("receiver_id"),
                rs.getString("receiver_mobile")
        ), tenantId, projectId);
    }

    public void insertMessage(Long messageId, Long tenantId, Long projectId, NoticeRecipient recipient,
                              String templateCode, String title, String content) {
        jdbcTemplate.update("""
                        INSERT INTO message_record(message_id, tenant_id, project_id, receiver_type, receiver_id,
                                                   receiver_mobile, channel, template_code, title, content, send_status)
                        VALUES (?, ?, ?, ?, ?, ?, 'SITE', ?, ?, ?, 'PENDING')
                        """, messageId, tenantId, projectId, recipient.receiverType(), recipient.receiverId(),
                recipient.receiverMobile(), templateCode, title, content);
    }

    public int dispatchPendingSiteMessages(Long tenantId, int limit) {
        return jdbcTemplate.update("""
                UPDATE message_record
                SET send_status = 'SENT', sent_at = NOW(), fail_reason = NULL
                WHERE tenant_id = ? AND channel = 'SITE' AND send_status = 'PENDING'
                ORDER BY created_at ASC, message_id ASC
                LIMIT ?
                """, tenantId, limit);
    }

    public record WorkOrderSlaCandidate(Long workOrderId, Long projectId, String orderNo, String status,
                                        String title, LocalDateTime slaDeadline) {
    }

    public record PatrolMissedCandidate(Long taskId, Long projectId, String taskNo, String taskName,
                                        LocalDateTime plannedEndAt) {
    }

    public record LeaseExpireCandidate(Long contractId, Long projectId, String contractNo, LocalDate endDate) {
    }
}
