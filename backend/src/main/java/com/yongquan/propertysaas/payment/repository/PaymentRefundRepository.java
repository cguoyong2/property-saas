package com.yongquan.propertysaas.payment.repository;

import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.ReconcileSummaryView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRefundRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public PaymentRefundRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<PayRefundView> findRefunds(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                           String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT refund_id, project_id, refund_no, order_id, transaction_id, refund_amount, reason, status,
                       third_refund_no, refunded_at, apply_user_id, audit_user_id, audit_at, created_at
                FROM pay_refund
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendRefundFilters(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, refund_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapRefund, args.toArray());
    }

    public long countRefunds(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pay_refund WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendRefundFilters(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public PayRefundView getRefund(Long tenantId, Long refundId) {
        return jdbcTemplate.queryForObject("""
                SELECT refund_id, project_id, refund_no, order_id, transaction_id, refund_amount, reason, status,
                       third_refund_no, refunded_at, apply_user_id, audit_user_id, audit_at, created_at
                FROM pay_refund
                WHERE tenant_id = ? AND refund_id = ? AND deleted = 0
                """, this::mapRefund, tenantId, refundId);
    }

    public PayRefundView getRefundByNo(String refundNo) {
        return jdbcTemplate.queryForObject("""
                SELECT refund_id, project_id, refund_no, order_id, transaction_id, refund_amount, reason, status,
                       third_refund_no, refunded_at, apply_user_id, audit_user_id, audit_at, created_at
                FROM pay_refund
                WHERE refund_no = ? AND deleted = 0
                """, this::mapRefund, refundNo);
    }

    public PayOrderView getOrder(Long tenantId, Long orderId) {
        return jdbcTemplate.queryForObject("""
                SELECT order_id, tenant_id, project_id, order_no, member_id, pay_channel, amount, subject, status,
                       expire_at, paid_at, third_trade_no, created_at
                FROM pay_order
                WHERE tenant_id = ? AND order_id = ? AND deleted = 0
                """, this::mapOrder, tenantId, orderId);
    }

    public Long findTransactionId(Long tenantId, Long orderId) {
        return jdbcTemplate.queryForObject("""
                SELECT transaction_id
                FROM pay_transaction
                WHERE tenant_id = ? AND order_id = ?
                ORDER BY paid_at DESC, transaction_id DESC
                LIMIT 1
                """, Long.class, tenantId, orderId);
    }

    public BigDecimal refundableAmount(Long tenantId, Long orderId) {
        BigDecimal paid = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount), 0)
                FROM pay_transaction
                WHERE tenant_id = ? AND order_id = ?
                """, BigDecimal.class, tenantId, orderId);
        BigDecimal frozenOrRefunded = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0)
                FROM pay_refund
                WHERE tenant_id = ? AND order_id = ? AND deleted = 0
                  AND status NOT IN ('AUDIT_REJECTED', 'FAILED', 'CLOSED')
                """, BigDecimal.class, tenantId, orderId);
        return paid.subtract(frozenOrRefunded);
    }

    public void insertRefund(Long tenantId, Long projectId, Long refundId, String refundNo, Long orderId,
                             Long transactionId, BigDecimal refundAmount, String reason, Long userId) {
        jdbcTemplate.update("""
                        INSERT INTO pay_refund(refund_id, tenant_id, project_id, refund_no, order_id, transaction_id,
                                               refund_amount, reason, status, apply_user_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APPLYING', ?)
                        """, refundId, tenantId, projectId, refundNo, orderId, transactionId, refundAmount, reason, userId);
    }

    public int auditRefund(Long tenantId, Long refundId, String status, Long userId) {
        return jdbcTemplate.update("""
                        UPDATE pay_refund
                        SET status = ?, audit_user_id = ?, audit_at = NOW()
                        WHERE tenant_id = ? AND refund_id = ? AND deleted = 0 AND status = 'APPLYING'
                        """, status, userId, tenantId, refundId);
    }

    public boolean refundTransactionExists(Long tenantId, String payChannel, String thirdRefundNo) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pay_refund_transaction
                WHERE tenant_id = ? AND pay_channel = ? AND third_refund_no = ?
                """, Integer.class, tenantId, payChannel, thirdRefundNo);
        return count != null && count > 0;
    }

    public boolean insertRefundTransactionIfAbsent(Long refundTxId, Long tenantId, Long projectId, Long refundId,
                                                   String refundNo, String thirdRefundNo, String payChannel,
                                                   BigDecimal refundAmount, LocalDateTime refundedAt, String rawNotify) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO pay_refund_transaction(refund_tx_id, tenant_id, project_id, refund_id,
                                                               refund_no, third_refund_no, pay_channel,
                                                               refund_amount, refunded_at, raw_notify)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                            """, refundTxId, tenantId, projectId, refundId, refundNo, thirdRefundNo, payChannel,
                    refundAmount, refundedAt, rawNotify);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void markRefundRefunded(Long tenantId, Long refundId, String thirdRefundNo, LocalDateTime refundedAt, String rawNotify) {
        jdbcTemplate.update("""
                        UPDATE pay_refund
                        SET status = 'REFUNDED', third_refund_no = ?, refunded_at = ?, raw_notify = CAST(? AS JSON)
                        WHERE tenant_id = ? AND refund_id = ? AND deleted = 0
                        """, thirdRefundNo, refundedAt, rawNotify, tenantId, refundId);
    }

    public List<PayableBill> findOrderBills(Long tenantId, Long orderId) {
        return jdbcTemplate.query("""
                SELECT b.bill_id, b.project_id, b.bill_no, b.member_id,
                       LEAST(ob.amount, GREATEST(b.paid_amount - b.refund_amount, 0)) AS remaining_amount,
                       b.status
                FROM pay_order_bill ob
                JOIN fee_bill b ON b.tenant_id = ob.tenant_id AND b.bill_id = ob.bill_id
                WHERE ob.tenant_id = ? AND ob.order_id = ? AND b.deleted = 0
                ORDER BY ob.id ASC
                """, this::mapPayableBill, tenantId, orderId);
    }

    public void refundBill(Long tenantId, Long billId, BigDecimal amount) {
        jdbcTemplate.update("""
                        UPDATE fee_bill
                        SET refund_amount = refund_amount + ?,
                            status = CASE
                                WHEN refund_amount + ? >= paid_amount THEN 'REFUNDED'
                                ELSE 'PARTIAL_REFUNDED'
                            END
                        WHERE tenant_id = ? AND bill_id = ? AND deleted = 0
                        """, amount, amount, tenantId, billId);
    }

    public void markOrderRefundStatus(Long tenantId, Long orderId) {
        jdbcTemplate.update("""
                        UPDATE pay_order o
                        SET status = CASE
                            WHEN (SELECT COALESCE(SUM(refund_amount), 0)
                                  FROM pay_refund r
                                  WHERE r.tenant_id = o.tenant_id AND r.order_id = o.order_id
                                    AND r.deleted = 0 AND r.status = 'REFUNDED') >= o.amount
                            THEN 'REFUNDED'
                            ELSE 'PARTIAL_REFUNDED'
                        END
                        WHERE o.tenant_id = ? AND o.order_id = ? AND o.deleted = 0
                        """, tenantId, orderId);
    }

    public ReconcileSummaryView reconcile(Long tenantId, List<Long> allowedProjectIds, Long projectId) {
        String projectFilter = projectId == null ? "" : " AND project_id = ?";
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (projectId != null) {
            args.add(projectId);
        }
        BigDecimal paidAmount = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM pay_transaction WHERE tenant_id = ?"
                + projectFilter + projectScopeSql(allowedProjectIds), BigDecimal.class, scopedArgs(args, allowedProjectIds));
        BigDecimal refundAmount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0)
                FROM pay_refund_transaction
                WHERE tenant_id = ?
                """ + projectFilter + projectScopeSql(allowedProjectIds), BigDecimal.class, scopedArgs(args, allowedProjectIds));
        BigDecimal orderPaidAmount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount), 0)
                FROM pay_order
                WHERE tenant_id = ? AND deleted = 0 AND status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                """ + projectFilter + projectScopeSql(allowedProjectIds), BigDecimal.class, scopedArgs(args, allowedProjectIds));
        BigDecimal netAmount = paidAmount.subtract(refundAmount);
        BigDecimal exceptionAmount = orderPaidAmount.subtract(paidAmount).abs();
        return new ReconcileSummaryView(projectId, paidAmount, refundAmount, netAmount, orderPaidAmount, exceptionAmount);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public String findPaySecret(Long tenantId, Long projectId, String payChannel) {
        return jdbcTemplate.queryForObject("""
                SELECT api_v3_key_encrypted
                FROM tenant_pay_config
                WHERE tenant_id = ? AND pay_channel = ? AND status = 'ACTIVE'
                  AND (project_id = ? OR project_id IS NULL)
                ORDER BY project_id IS NULL ASC, pay_config_id DESC
                LIMIT 1
                """, String.class, tenantId, payChannel, projectId);
    }

    private void appendRefundFilters(StringBuilder sql, List<Object> args, Long projectId, String status) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private String projectScopeSql(List<Long> allowedProjectIds) {
        if (allowedProjectIds == null) {
            return "";
        }
        if (allowedProjectIds.isEmpty()) {
            return " AND 1 = 0";
        }
        return " AND project_id IN (" + "?,".repeat(allowedProjectIds.size()).replaceFirst(",$", "") + ")";
    }

    private Object[] scopedArgs(List<Object> args, List<Long> allowedProjectIds) {
        List<Object> scoped = new ArrayList<>(args);
        if (allowedProjectIds != null && !allowedProjectIds.isEmpty()) {
            scoped.addAll(allowedProjectIds);
        }
        return scoped.toArray();
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

    private PayRefundView mapRefund(ResultSet rs, int rowNum) throws SQLException {
        return new PayRefundView(rs.getLong("refund_id"), rs.getLong("project_id"), rs.getString("refund_no"),
                rs.getLong("order_id"), (Long) rs.getObject("transaction_id"), rs.getBigDecimal("refund_amount"),
                rs.getString("reason"), rs.getString("status"), rs.getString("third_refund_no"),
                toLocalDateTime(rs, "refunded_at"), (Long) rs.getObject("apply_user_id"),
                (Long) rs.getObject("audit_user_id"), toLocalDateTime(rs, "audit_at"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PayOrderView mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new PayOrderView(rs.getLong("order_id"), rs.getLong("tenant_id"), rs.getLong("project_id"),
                rs.getString("order_no"), (Long) rs.getObject("member_id"), rs.getString("pay_channel"),
                rs.getBigDecimal("amount"), rs.getString("subject"), rs.getString("status"), toLocalDateTime(rs, "expire_at"),
                toLocalDateTime(rs, "paid_at"), rs.getString("third_trade_no"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PayableBill mapPayableBill(ResultSet rs, int rowNum) throws SQLException {
        return new PayableBill(rs.getLong("bill_id"), rs.getLong("project_id"), rs.getString("bill_no"),
                (Long) rs.getObject("member_id"), rs.getBigDecimal("remaining_amount"), rs.getString("status"));
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
