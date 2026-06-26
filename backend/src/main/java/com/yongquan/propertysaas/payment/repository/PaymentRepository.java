package com.yongquan.propertysaas.payment.repository;

import com.yongquan.propertysaas.payment.domain.MemberPrepaymentView;
import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayTransactionView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
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
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public PaymentRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<PayOrderView> findOrders(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                         String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT o.order_id, o.tenant_id, o.project_id, o.order_no, o.member_id, o.pay_channel, o.amount,
                       o.subject, o.status, o.expire_at, o.paid_at, o.third_trade_no, o.created_at,
                       m.real_name AS member_name, m.mobile AS member_mobile,
                       COALESCE(ob.bill_count, 0) AS bill_count,
                       COALESCE(ob.bill_applied_amount, 0) AS bill_applied_amount,
                       COALESCE(tx.transaction_amount, 0) AS transaction_amount,
                       COALESCE(pp.prepayment_amount, 0) AS prepayment_amount,
                       COALESCE(pp.prepayment_remaining_amount, 0) AS prepayment_remaining_amount
                FROM pay_order o
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT tenant_id, order_id, COUNT(*) AS bill_count, SUM(amount) AS bill_applied_amount
                    FROM pay_order_bill
                    GROUP BY tenant_id, order_id
                ) ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS transaction_amount
                    FROM pay_transaction
                    GROUP BY tenant_id, order_id
                ) tx ON tx.tenant_id = o.tenant_id AND tx.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount,
                           SUM(remaining_amount) AS prepayment_remaining_amount
                    FROM member_prepayment
                    WHERE deleted = 0
                    GROUP BY tenant_id, order_id
                ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                WHERE o.tenant_id = ? AND o.deleted = 0
                """);
        args.add(tenantId);
        appendOrderFilters(sql, args, projectId, status, "o.project_id", "o.status");
        appendProjectScope(sql, args, allowedProjectIds, "o.project_id");
        sql.append(" ORDER BY o.created_at DESC, o.order_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapOrder, args.toArray());
    }

    public long countOrders(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pay_order WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendOrderFilters(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<PayTransactionView> findTransactions(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                     long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT t.transaction_id, t.project_id, t.order_id, t.order_no, t.third_trade_no, t.pay_channel,
                       t.amount, t.paid_at, t.created_at, o.member_id, m.real_name AS member_name,
                       m.mobile AS member_mobile, o.status AS order_status, o.amount AS order_amount,
                       COALESCE(pp.prepayment_amount, 0) AS prepayment_amount
                FROM pay_transaction t
                LEFT JOIN pay_order o ON o.tenant_id = t.tenant_id AND o.order_id = t.order_id AND o.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount
                    FROM member_prepayment
                    WHERE deleted = 0
                    GROUP BY tenant_id, order_id
                ) pp ON pp.tenant_id = t.tenant_id AND pp.order_id = t.order_id
                WHERE t.tenant_id = ?
                """);
        args.add(tenantId);
        if (projectId != null) {
            sql.append(" AND t.project_id = ?");
            args.add(projectId);
        }
        appendProjectScope(sql, args, allowedProjectIds, "t.project_id");
        sql.append(" ORDER BY t.paid_at DESC, t.transaction_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapTransaction, args.toArray());
    }

    public long countTransactions(Long tenantId, List<Long> allowedProjectIds, Long projectId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pay_transaction WHERE tenant_id = ?");
        args.add(tenantId);
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public List<MemberPrepaymentView> findPrepayments(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                      String memberName, String orderNo, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT p.prepayment_id, p.project_id, bp.project_name, p.member_id,
                       m.real_name AS member_name, m.mobile, p.order_id, p.order_no, p.amount,
                       p.amount - p.remaining_amount AS used_amount, p.remaining_amount,
                       p.source, p.remark, p.created_at
                FROM member_prepayment p
                LEFT JOIN base_project bp ON bp.tenant_id = p.tenant_id
                    AND bp.project_id = p.project_id AND bp.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = p.tenant_id
                    AND m.member_id = p.member_id AND m.deleted = 0
                WHERE p.tenant_id = ? AND p.deleted = 0
                """);
        args.add(tenantId);
        appendPrepaymentFilters(sql, args, projectId, memberName, orderNo);
        appendProjectScope(sql, args, allowedProjectIds, "p.project_id");
        sql.append(" ORDER BY p.created_at DESC, p.prepayment_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapPrepayment, args.toArray());
    }

    public long countPrepayments(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                 String memberName, String orderNo) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM member_prepayment p
                LEFT JOIN member_user m ON m.tenant_id = p.tenant_id
                    AND m.member_id = p.member_id AND m.deleted = 0
                WHERE p.tenant_id = ? AND p.deleted = 0
                """);
        args.add(tenantId);
        appendPrepaymentFilters(sql, args, projectId, memberName, orderNo);
        appendProjectScope(sql, args, allowedProjectIds, "p.project_id");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public PayOrderView getOrderByNo(String orderNo) {
        return jdbcTemplate.queryForObject("""
                SELECT o.order_id, o.tenant_id, o.project_id, o.order_no, o.member_id, o.pay_channel, o.amount,
                       o.subject, o.status, o.expire_at, o.paid_at, o.third_trade_no, o.created_at,
                       m.real_name AS member_name, m.mobile AS member_mobile,
                       COALESCE(ob.bill_count, 0) AS bill_count,
                       COALESCE(ob.bill_applied_amount, 0) AS bill_applied_amount,
                       COALESCE(tx.transaction_amount, 0) AS transaction_amount,
                       COALESCE(pp.prepayment_amount, 0) AS prepayment_amount,
                       COALESCE(pp.prepayment_remaining_amount, 0) AS prepayment_remaining_amount
                FROM pay_order o
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT tenant_id, order_id, COUNT(*) AS bill_count, SUM(amount) AS bill_applied_amount
                    FROM pay_order_bill
                    GROUP BY tenant_id, order_id
                ) ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS transaction_amount
                    FROM pay_transaction
                    GROUP BY tenant_id, order_id
                ) tx ON tx.tenant_id = o.tenant_id AND tx.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount,
                           SUM(remaining_amount) AS prepayment_remaining_amount
                    FROM member_prepayment
                    WHERE deleted = 0
                    GROUP BY tenant_id, order_id
                ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                WHERE o.order_no = ? AND o.deleted = 0
                """, this::mapOrder, orderNo);
    }

    public List<PayableBill> findPayableBills(Long tenantId, Long projectId, List<Long> billIds) {
        StringBuilder sql = new StringBuilder("""
                SELECT bill_id, project_id, bill_no, member_id, remaining_amount, status
                FROM fee_bill
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0 AND bill_id IN (
                """);
        sql.append("?,".repeat(billIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(") ORDER BY bill_id ASC");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(projectId);
        args.addAll(billIds);
        return jdbcTemplate.query(sql.toString(), this::mapPayableBill, args.toArray());
    }

    public void insertOrder(Long tenantId, Long orderId, String orderNo, Long userId, Long projectId, Long memberId,
                            String payChannel, BigDecimal amount, String subject, LocalDateTime expireAt) {
        jdbcTemplate.update("""
                        INSERT INTO pay_order(order_id, tenant_id, project_id, order_no, member_id, pay_channel,
                                              amount, subject, status, expire_at, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAYING', ?, ?)
                        """, orderId, tenantId, projectId, orderNo, memberId, payChannel, amount, subject, expireAt, userId);
    }

    public void insertOrderBill(Long id, Long tenantId, Long projectId, Long orderId, Long billId, BigDecimal amount) {
        jdbcTemplate.update("""
                        INSERT INTO pay_order_bill(id, tenant_id, project_id, order_id, bill_id, amount)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, id, tenantId, projectId, orderId, billId, amount);
    }

    public int markBillsPaying(Long tenantId, Long projectId, List<Long> billIds) {
        StringBuilder sql = new StringBuilder("""
                UPDATE fee_bill
                SET status = 'PAYING'
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0
                  AND status IN ('UNPAID', 'OVERDUE', 'PARTIAL_PAID') AND bill_id IN (
                """);
        sql.append("?,".repeat(billIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(projectId);
        args.addAll(billIds);
        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    public List<PayableBill> findOrderBills(Long tenantId, Long orderId) {
        return jdbcTemplate.query("""
                SELECT b.bill_id, b.project_id, b.bill_no, b.member_id, ob.amount AS remaining_amount, b.status
                FROM pay_order_bill ob
                JOIN fee_bill b ON b.tenant_id = ob.tenant_id AND b.bill_id = ob.bill_id
                WHERE ob.tenant_id = ? AND ob.order_id = ? AND b.deleted = 0
                ORDER BY ob.id ASC
                """, this::mapPayableBill, tenantId, orderId);
    }

    public boolean transactionExists(Long tenantId, String payChannel, String thirdTradeNo) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pay_transaction
                WHERE tenant_id = ? AND pay_channel = ? AND third_trade_no = ?
                """, Integer.class, tenantId, payChannel, thirdTradeNo);
        return count != null && count > 0;
    }

    public boolean insertTransactionIfAbsent(Long transactionId, Long tenantId, Long projectId, Long orderId,
                                             String orderNo, String thirdTradeNo, String payChannel,
                                             BigDecimal amount, LocalDateTime paidAt, String rawNotify) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO pay_transaction(transaction_id, tenant_id, project_id, order_id, order_no,
                                                        third_trade_no, pay_channel, amount, paid_at, raw_notify)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                            """, transactionId, tenantId, projectId, orderId, orderNo, thirdTradeNo, payChannel,
                    amount, paidAt, rawNotify);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void insertPrepayment(Long prepaymentId, Long tenantId, Long projectId, Long memberId, Long orderId,
                                 String orderNo, BigDecimal amount, Long userId, String remark) {
        jdbcTemplate.update("""
                        INSERT INTO member_prepayment(prepayment_id, tenant_id, project_id, member_id, order_id,
                                                      order_no, amount, remaining_amount, source, remark, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OFFLINE_OVERPAY', ?, ?)
                        """, prepaymentId, tenantId, projectId, memberId, orderId, orderNo, amount, amount, remark, userId);
    }

    public void markOrderPaid(Long tenantId, Long orderId, String thirdTradeNo, LocalDateTime paidAt) {
        jdbcTemplate.update("""
                        UPDATE pay_order
                        SET status = 'PAID', paid_at = ?, third_trade_no = ?
                        WHERE tenant_id = ? AND order_id = ? AND deleted = 0 AND status <> 'PAID'
                        """, paidAt, thirdTradeNo, tenantId, orderId);
    }

    public void settleBill(Long tenantId, Long billId, BigDecimal amount) {
        jdbcTemplate.update("""
                        UPDATE fee_bill
                        SET paid_amount = paid_amount + ?,
                            remaining_amount = GREATEST(remaining_amount - ?, 0),
                            status = CASE
                                WHEN GREATEST(remaining_amount - ?, 0) = 0 THEN 'PAID'
                                ELSE 'PARTIAL_PAID'
                            END
                        WHERE tenant_id = ? AND bill_id = ? AND deleted = 0
                        """, amount, amount, amount, tenantId, billId);
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

    private void appendOrderFilters(StringBuilder sql, List<Object> args, Long projectId, String status) {
        appendOrderFilters(sql, args, projectId, status, "project_id", "status");
    }

    private void appendOrderFilters(StringBuilder sql, List<Object> args, Long projectId, String status,
                                    String projectColumn, String statusColumn) {
        if (projectId != null) {
            sql.append(" AND ").append(projectColumn).append(" = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND ").append(statusColumn).append(" = ?");
            args.add(status);
        }
    }

    private void appendPrepaymentFilters(StringBuilder sql, List<Object> args, Long projectId, String memberName, String orderNo) {
        if (projectId != null) {
            sql.append(" AND p.project_id = ?");
            args.add(projectId);
        }
        if (memberName != null && !memberName.isBlank()) {
            sql.append(" AND (m.real_name LIKE ? OR m.mobile LIKE ?)");
            String keyword = "%" + memberName.trim() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            sql.append(" AND p.order_no LIKE ?");
            args.add("%" + orderNo.trim() + "%");
        }
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds, String column) {
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

    private PayOrderView mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new PayOrderView(rs.getLong("order_id"), rs.getLong("tenant_id"), rs.getLong("project_id"),
                rs.getString("order_no"), (Long) rs.getObject("member_id"), rs.getString("pay_channel"),
                rs.getBigDecimal("amount"), rs.getString("subject"), rs.getString("status"), toLocalDateTime(rs, "expire_at"),
                toLocalDateTime(rs, "paid_at"), rs.getString("third_trade_no"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("member_name"),
                rs.getString("member_mobile"), (Long) rs.getObject("bill_count"), rs.getBigDecimal("bill_applied_amount"),
                rs.getBigDecimal("transaction_amount"), rs.getBigDecimal("prepayment_amount"),
                rs.getBigDecimal("prepayment_remaining_amount"));
    }

    private PayTransactionView mapTransaction(ResultSet rs, int rowNum) throws SQLException {
        return new PayTransactionView(rs.getLong("transaction_id"), rs.getLong("project_id"), rs.getLong("order_id"),
                rs.getString("order_no"), rs.getString("third_trade_no"), rs.getString("pay_channel"),
                rs.getBigDecimal("amount"), rs.getTimestamp("paid_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime(), (Long) rs.getObject("member_id"),
                rs.getString("member_name"), rs.getString("member_mobile"), rs.getString("order_status"),
                rs.getBigDecimal("order_amount"), rs.getBigDecimal("prepayment_amount"));
    }

    private MemberPrepaymentView mapPrepayment(ResultSet rs, int rowNum) throws SQLException {
        return new MemberPrepaymentView(rs.getLong("prepayment_id"), rs.getLong("project_id"),
                rs.getString("project_name"), (Long) rs.getObject("member_id"), rs.getString("member_name"),
                rs.getString("mobile"), (Long) rs.getObject("order_id"), rs.getString("order_no"),
                rs.getBigDecimal("amount"), rs.getBigDecimal("used_amount"), rs.getBigDecimal("remaining_amount"),
                rs.getString("source"), rs.getString("remark"), rs.getTimestamp("created_at").toLocalDateTime());
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
