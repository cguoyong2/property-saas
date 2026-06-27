package com.yongquan.propertysaas.payment.repository;

import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.domain.PayableBill;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionHistoryView;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionReviewView;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionStatsView;
import com.yongquan.propertysaas.payment.domain.ReconcileExceptionView;
import com.yongquan.propertysaas.payment.domain.ReconcileReviewStatsView;
import com.yongquan.propertysaas.payment.domain.ReconcileSummaryView;
import com.yongquan.propertysaas.payment.domain.RefundableOrderView;
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
                                           String refundNo, String orderNo, String memberName, String status,
                                           long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT r.refund_id, r.project_id, r.refund_no, r.order_id, o.order_no, r.transaction_id,
                       r.refund_amount, r.reason, r.status, r.third_refund_no, r.refunded_at,
                       r.apply_user_id, r.audit_user_id, r.audit_at, r.created_at,
                       o.member_id, m.real_name AS member_name, m.mobile AS member_mobile,
                       hs.house_no, hs.bill_summary, o.pay_channel, o.amount AS order_amount,
                       COALESCE(rt.refunded_transaction_amount, 0) AS refunded_transaction_amount
                FROM pay_refund r
                LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id,
                           GROUP_CONCAT(DISTINCT CONCAT_WS('', bd.building_name, u.unit_name, h.house_no)
                                        ORDER BY bd.building_name, u.unit_name, h.house_no SEPARATOR '、') AS house_no,
                           GROUP_CONCAT(DISTINCT CONCAT(COALESCE(fi.item_name, '费用'), ' ', fb.bill_period, ' ', ob.amount, '元')
                                        ORDER BY fb.bill_period, fi.item_name SEPARATOR '；') AS bill_summary
                    FROM pay_order_bill ob
                    LEFT JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                    LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                    LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                    LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                    GROUP BY ob.tenant_id, ob.order_id
                ) hs ON hs.tenant_id = r.tenant_id AND hs.order_id = r.order_id
                LEFT JOIN (
                    SELECT tenant_id, refund_id, SUM(refund_amount) AS refunded_transaction_amount
                    FROM pay_refund_transaction
                    GROUP BY tenant_id, refund_id
                ) rt ON rt.tenant_id = r.tenant_id AND rt.refund_id = r.refund_id
                WHERE r.tenant_id = ? AND r.deleted = 0
                """);
        args.add(tenantId);
        appendRefundFilters(sql, args, projectId, refundNo, orderNo, memberName, status);
        appendProjectScope(sql, args, allowedProjectIds, "r.project_id");
        sql.append(" ORDER BY r.created_at DESC, r.refund_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapRefund, args.toArray());
    }

    public long countRefunds(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                             String refundNo, String orderNo, String memberName, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM pay_refund r
                LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                WHERE r.tenant_id = ? AND r.deleted = 0
                """);
        args.add(tenantId);
        appendRefundFilters(sql, args, projectId, refundNo, orderNo, memberName, status);
        appendProjectScope(sql, args, allowedProjectIds, "r.project_id");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public PayRefundView getRefund(Long tenantId, Long refundId) {
        return jdbcTemplate.queryForObject("""
                SELECT r.refund_id, r.project_id, r.refund_no, r.order_id, o.order_no, r.transaction_id,
                       r.refund_amount, r.reason, r.status, r.third_refund_no, r.refunded_at,
                       r.apply_user_id, r.audit_user_id, r.audit_at, r.created_at,
                       o.member_id, m.real_name AS member_name, m.mobile AS member_mobile,
                       hs.house_no, hs.bill_summary, o.pay_channel, o.amount AS order_amount,
                       COALESCE(rt.refunded_transaction_amount, 0) AS refunded_transaction_amount
                FROM pay_refund r
                LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id,
                           GROUP_CONCAT(DISTINCT CONCAT_WS('', bd.building_name, u.unit_name, h.house_no)
                                        ORDER BY bd.building_name, u.unit_name, h.house_no SEPARATOR '、') AS house_no,
                           GROUP_CONCAT(DISTINCT CONCAT(COALESCE(fi.item_name, '费用'), ' ', fb.bill_period, ' ', ob.amount, '元')
                                        ORDER BY fb.bill_period, fi.item_name SEPARATOR '；') AS bill_summary
                    FROM pay_order_bill ob
                    LEFT JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                    LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                    LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                    LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                    GROUP BY ob.tenant_id, ob.order_id
                ) hs ON hs.tenant_id = r.tenant_id AND hs.order_id = r.order_id
                LEFT JOIN (
                    SELECT tenant_id, refund_id, SUM(refund_amount) AS refunded_transaction_amount
                    FROM pay_refund_transaction
                    GROUP BY tenant_id, refund_id
                ) rt ON rt.tenant_id = r.tenant_id AND rt.refund_id = r.refund_id
                WHERE r.tenant_id = ? AND r.refund_id = ? AND r.deleted = 0
                """, this::mapRefund, tenantId, refundId);
    }

    public PayRefundView getRefundByNo(String refundNo) {
        return jdbcTemplate.queryForObject("""
                SELECT r.refund_id, r.project_id, r.refund_no, r.order_id, o.order_no, r.transaction_id,
                       r.refund_amount, r.reason, r.status, r.third_refund_no, r.refunded_at,
                       r.apply_user_id, r.audit_user_id, r.audit_at, r.created_at,
                       o.member_id, m.real_name AS member_name, m.mobile AS member_mobile,
                       hs.house_no, hs.bill_summary, o.pay_channel, o.amount AS order_amount,
                       COALESCE(rt.refunded_transaction_amount, 0) AS refunded_transaction_amount
                FROM pay_refund r
                LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id,
                           GROUP_CONCAT(DISTINCT CONCAT_WS('', bd.building_name, u.unit_name, h.house_no)
                                        ORDER BY bd.building_name, u.unit_name, h.house_no SEPARATOR '、') AS house_no,
                           GROUP_CONCAT(DISTINCT CONCAT(COALESCE(fi.item_name, '费用'), ' ', fb.bill_period, ' ', ob.amount, '元')
                                        ORDER BY fb.bill_period, fi.item_name SEPARATOR '；') AS bill_summary
                    FROM pay_order_bill ob
                    LEFT JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                    LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                    LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                    LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                    GROUP BY ob.tenant_id, ob.order_id
                ) hs ON hs.tenant_id = r.tenant_id AND hs.order_id = r.order_id
                LEFT JOIN (
                    SELECT tenant_id, refund_id, SUM(refund_amount) AS refunded_transaction_amount
                    FROM pay_refund_transaction
                    GROUP BY tenant_id, refund_id
                ) rt ON rt.tenant_id = r.tenant_id AND rt.refund_id = r.refund_id
                WHERE r.refund_no = ? AND r.deleted = 0
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

    public List<RefundableOrderView> findRefundableOrders(Long tenantId, List<Long> allowedProjectIds,
                                                          Long projectId, Long memberId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT o.order_id, o.order_no, o.project_id, o.member_id,
                       m.real_name AS member_name, m.mobile,
                       GROUP_CONCAT(DISTINCT CONCAT_WS('', bd.building_name, u.unit_name, h.house_no)
                                    ORDER BY bd.building_name, u.unit_name, h.house_no SEPARATOR '、') AS house_no,
                       o.amount,
                       COALESCE(rr.refunded_amount, 0) AS refunded_amount,
                       GREATEST(COALESCE(ba.bill_refundable_amount, 0)
                           + COALESCE(pp.prepayment_remaining_amount, 0)
                           - COALESCE(pr.pending_refund_amount, 0), 0) AS refundable_amount,
                       GROUP_CONCAT(DISTINCT CONCAT(fi.item_name, ' ', fb.bill_period, ' ', ob.amount, '元')
                                    ORDER BY fb.bill_period, fi.item_name SEPARATOR '；') AS bill_summary,
                       o.paid_at
                FROM pay_order o
                LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                LEFT JOIN pay_order_bill ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                LEFT JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id,
                           SUM(LEAST(ob.amount, GREATEST(fb.paid_amount - fb.refund_amount, 0))) AS bill_refundable_amount
                    FROM pay_order_bill ob
                    JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    GROUP BY ob.tenant_id, ob.order_id
                ) ba ON ba.tenant_id = o.tenant_id AND ba.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, COALESCE(SUM(remaining_amount), 0) AS prepayment_remaining_amount
                    FROM member_prepayment
                    WHERE deleted = 0
                    GROUP BY tenant_id, order_id
                ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, COALESCE(SUM(refund_amount), 0) AS pending_refund_amount
                    FROM pay_refund
                    WHERE deleted = 0 AND status IN ('APPLYING', 'AUDIT_PASSED', 'REFUNDING')
                    GROUP BY tenant_id, order_id
                ) pr ON pr.tenant_id = o.tenant_id AND pr.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, COALESCE(SUM(refund_amount), 0) AS refunded_amount
                    FROM pay_refund
                    WHERE deleted = 0 AND status = 'REFUNDED'
                    GROUP BY tenant_id, order_id
                ) rr ON rr.tenant_id = o.tenant_id AND rr.order_id = o.order_id
                WHERE o.tenant_id = ? AND o.deleted = 0 AND o.status IN ('PAID', 'PARTIAL_REFUNDED')
                """);
        args.add(tenantId);
        if (projectId != null) {
            sql.append(" AND o.project_id = ?");
            args.add(projectId);
        }
        if (memberId != null) {
            sql.append(" AND o.member_id = ?");
            args.add(memberId);
        }
        appendProjectScope(sql, args, allowedProjectIds, "o.project_id");
        sql.append("""

                GROUP BY o.order_id, o.order_no, o.project_id, o.member_id, m.real_name, m.mobile,
                         o.amount, rr.refunded_amount, ba.bill_refundable_amount, pp.prepayment_remaining_amount,
                         pr.pending_refund_amount, o.paid_at
                HAVING refundable_amount > 0
                ORDER BY o.paid_at DESC, o.order_id DESC
                LIMIT 100
                """);
        return jdbcTemplate.query(sql.toString(), this::mapRefundableOrder, args.toArray());
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
        BigDecimal available = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refundable_amount), 0)
                FROM (
                    SELECT LEAST(ob.amount, GREATEST(fb.paid_amount - fb.refund_amount, 0)) AS refundable_amount
                    FROM pay_order_bill ob
                    JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    WHERE ob.tenant_id = ? AND ob.order_id = ?
                    UNION ALL
                    SELECT remaining_amount AS refundable_amount
                    FROM member_prepayment
                    WHERE tenant_id = ? AND order_id = ? AND deleted = 0 AND remaining_amount > 0
                ) a
                """, BigDecimal.class, tenantId, orderId, tenantId, orderId);
        BigDecimal pending = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0)
                FROM pay_refund
                WHERE tenant_id = ? AND order_id = ? AND deleted = 0
                  AND status IN ('APPLYING', 'AUDIT_PASSED', 'REFUNDING')
                """, BigDecimal.class, tenantId, orderId);
        return available.subtract(pending).max(BigDecimal.ZERO);
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

    public BigDecimal refundPrepayments(Long tenantId, Long orderId, BigDecimal refundAmount) {
        BigDecimal remaining = refundAmount;
        for (PrepaymentForRefund prepayment : jdbcTemplate.query("""
                SELECT prepayment_id, remaining_amount
                FROM member_prepayment
                WHERE tenant_id = ? AND order_id = ? AND deleted = 0 AND remaining_amount > 0
                ORDER BY created_at ASC, prepayment_id ASC
                """, (rs, rowNum) -> new PrepaymentForRefund(
                rs.getLong("prepayment_id"),
                rs.getBigDecimal("remaining_amount")), tenantId, orderId)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal amount = prepayment.remainingAmount().min(remaining).setScale(2, java.math.RoundingMode.HALF_UP);
            int updated = jdbcTemplate.update("""
                            UPDATE member_prepayment
                            SET refunded_amount = refunded_amount + ?,
                                remaining_amount = remaining_amount - ?
                            WHERE tenant_id = ? AND prepayment_id = ? AND deleted = 0 AND remaining_amount >= ?
                            """, amount, amount, tenantId, prepayment.prepaymentId(), amount);
            if (updated != 1) {
                throw new IllegalStateException("预存款退款失败，余额已变化：" + prepayment.prepaymentId());
            }
            remaining = remaining.subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return refundAmount.subtract(remaining).setScale(2, java.math.RoundingMode.HALF_UP);
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

    public ReconcileSummaryView reconcile(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                          String startDate, String endDate, String payChannel, String orderStatus) {
        SummaryNumber tx = summary("""
                SELECT COALESCE(SUM(t.amount), 0) AS amount, COUNT(*) AS total
                FROM pay_transaction t
                LEFT JOIN pay_order o ON o.tenant_id = t.tenant_id AND o.order_id = t.order_id AND o.deleted = 0
                WHERE t.tenant_id = ?
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, payChannel, orderStatus,
                "t.project_id", "t.paid_at", "t.pay_channel", "o.status");
        SummaryNumber refund = summary("""
                SELECT COALESCE(SUM(rt.refund_amount), 0) AS amount, COUNT(*) AS total
                FROM pay_refund_transaction rt
                LEFT JOIN pay_refund r ON r.tenant_id = rt.tenant_id AND r.refund_id = rt.refund_id AND r.deleted = 0
                LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                WHERE rt.tenant_id = ?
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, payChannel, orderStatus,
                "rt.project_id", "rt.refunded_at", "rt.pay_channel", "o.status");
        SummaryNumber order = summary("""
                SELECT COALESCE(SUM(o.amount), 0) AS amount, COUNT(*) AS total
                FROM pay_order o
                WHERE o.tenant_id = ? AND o.deleted = 0
                  AND o.status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, payChannel, orderStatus,
                "o.project_id", "o.paid_at", "o.pay_channel", "o.status");
        SummaryNumber billApplied = summary("""
                SELECT COALESCE(SUM(ob.amount), 0) AS amount, COUNT(*) AS total
                FROM pay_order_bill ob
                JOIN pay_order o ON o.tenant_id = ob.tenant_id AND o.order_id = ob.order_id AND o.deleted = 0
                WHERE ob.tenant_id = ?
                  AND o.status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, payChannel, orderStatus,
                "ob.project_id", "o.paid_at", "o.pay_channel", "o.status");
        SummaryNumber prepayment = summary("""
                SELECT COALESCE(SUM(p.amount), 0) AS amount, COUNT(*) AS total
                FROM member_prepayment p
                LEFT JOIN pay_order o ON o.tenant_id = p.tenant_id AND o.order_id = p.order_id AND o.deleted = 0
                WHERE p.tenant_id = ? AND p.deleted = 0
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, payChannel, orderStatus,
                "p.project_id", "p.created_at", "o.pay_channel", "o.status");
        SummaryNumber prepaymentUsed = summary("""
                SELECT COALESCE(SUM(u.amount), 0) AS amount, COUNT(*) AS total
                FROM member_prepayment_usage u
                WHERE u.tenant_id = ? AND u.deleted = 0
                """, tenantId, allowedProjectIds, projectId, startDate, endDate, null, null,
                "u.project_id", "u.created_at", null, null);
        long exceptionCount = countReconcileExceptions(tenantId, allowedProjectIds, projectId, null, null, null, null, "OPEN");
        BigDecimal exceptionAmount = sumReconcileExceptionAmount(tenantId, allowedProjectIds, projectId, null, "OPEN");
        return new ReconcileSummaryView(projectId, tx.total(), refund.total(), order.total(), tx.amount(),
                refund.amount(), prepayment.amount(), prepaymentUsed.amount(), billApplied.amount(),
                tx.amount().subtract(refund.amount()), order.amount(), exceptionAmount, exceptionCount);
    }

    public List<ReconcileExceptionView> findReconcileExceptions(Long tenantId, List<Long> allowedProjectIds,
                                                                 Long projectId, String exceptionType, String exceptionLevel,
                                                                 String businessNo, String memberName, String status,
                                                                 long offset, long pageSize) {
        List<Object> args = exceptionArgs(tenantId, allowedProjectIds, projectId, exceptionType, exceptionLevel,
                businessNo, memberName, status);
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(exceptionSql(allowedProjectIds) + """
                ORDER BY created_at DESC, exception_key DESC
                LIMIT ? OFFSET ?
                """, this::mapReconcileException, args.toArray());
    }

    public long countReconcileExceptions(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                         String exceptionType, String exceptionLevel, String businessNo,
                                         String memberName, String status) {
        List<Object> args = exceptionArgs(tenantId, allowedProjectIds, projectId, exceptionType, exceptionLevel,
                businessNo, memberName, status);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + exceptionSql(allowedProjectIds) + ") e",
                Long.class, args.toArray());
        return value(count);
    }

    public ReconcileExceptionStatsView reconcileExceptionStats(Long tenantId, List<Long> allowedProjectIds,
                                                               Long projectId, String exceptionType,
                                                               String exceptionLevel, String businessNo,
                                                               String memberName, String status) {
        List<Object> args = exceptionArgs(tenantId, allowedProjectIds, projectId, exceptionType, exceptionLevel,
                businessNo, memberName, status);
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total_count,
                       COALESCE(SUM(CASE WHEN exception_level = '高' THEN 1 ELSE 0 END), 0) AS high_risk_count,
                       COALESCE(SUM(CASE WHEN exception_level = '中' THEN 1 ELSE 0 END), 0) AS medium_risk_count,
                       COALESCE(SUM(CASE WHEN exception_level = '低' THEN 1 ELSE 0 END), 0) AS low_risk_count,
                       COALESCE(SUM(CASE WHEN handle_status = 'OPEN' THEN 1 ELSE 0 END), 0) AS open_count,
                       COALESCE(SUM(CASE WHEN review_status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_review_count,
                       COALESCE(SUM(CASE WHEN handle_status = 'HANDLED' THEN 1 ELSE 0 END), 0) AS handled_count
                FROM (
                """ + exceptionSql(allowedProjectIds) + """
                ) e
                """, (rs, rowNum) -> new ReconcileExceptionStatsView(
                rs.getLong("total_count"),
                rs.getLong("high_risk_count"),
                rs.getLong("medium_risk_count"),
                rs.getLong("low_risk_count"),
                rs.getLong("open_count"),
                rs.getLong("pending_review_count"),
                rs.getLong("handled_count")), args.toArray());
    }

    public BigDecimal sumReconcileExceptionAmount(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                  String exceptionType, String status) {
        List<Object> args = exceptionArgs(tenantId, allowedProjectIds, projectId, exceptionType, null, null, null, status);
        BigDecimal amount = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM (" + exceptionSql(allowedProjectIds) + ") e",
                BigDecimal.class, args.toArray());
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public ReconcileExceptionView getReconcileException(Long tenantId, List<Long> allowedProjectIds, String exceptionKey) {
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        addExceptionSqlArgs(args, allowedProjectIds, null, null, null, null, null, null);
        args.add(exceptionKey);
        return jdbcTemplate.queryForObject("SELECT * FROM (" + exceptionSql(allowedProjectIds) + ") e WHERE e.exception_key = ?",
                this::mapReconcileException, args.toArray());
    }

    public boolean currentReconcileExceptionExists(Long tenantId, List<Long> allowedProjectIds, String exceptionKey) {
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        addExceptionSqlArgs(args, allowedProjectIds, null, null, null, null, null, null);
        args.add(exceptionKey);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + exceptionSql(allowedProjectIds)
                        + ") e WHERE e.exception_key = ?",
                Long.class, args.toArray());
        return value(count) > 0;
    }

    public List<ReconcileExceptionReviewView> findReconcileExceptionReviews(Long tenantId,
                                                                            List<Long> allowedProjectIds,
                                                                            Long projectId,
                                                                            String exceptionType,
                                                                            String memberName,
                                                                            String reviewStatus,
                                                                            String currentCheckStatus,
                                                                            long offset,
                                                                            long pageSize) {
        List<Object> args = reviewArgs(tenantId, allowedProjectIds, projectId, exceptionType, memberName,
                reviewStatus, currentCheckStatus);
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(reviewSql(allowedProjectIds) + """
                ORDER BY h.handled_at DESC, h.exception_key DESC
                LIMIT ? OFFSET ?
                """, this::mapReconcileExceptionReview, args.toArray());
    }

    public long countReconcileExceptionReviews(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                               String exceptionType, String memberName, String reviewStatus,
                                               String currentCheckStatus) {
        List<Object> args = reviewArgs(tenantId, allowedProjectIds, projectId, exceptionType, memberName,
                reviewStatus, currentCheckStatus);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + reviewSql(allowedProjectIds) + ") r",
                Long.class, args.toArray());
        return value(count);
    }

    public ReconcileReviewStatsView reconcileReviewStats(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                         String exceptionType, String memberName, String reviewStatus,
                                                         String currentCheckStatus) {
        List<Object> args = reviewArgs(tenantId, allowedProjectIds, projectId, exceptionType, memberName,
                reviewStatus, currentCheckStatus);
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total_count,
                       COALESCE(SUM(CASE WHEN review_status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_count,
                       COALESCE(SUM(CASE WHEN review_status = 'APPROVED' THEN 1 ELSE 0 END), 0) AS approved_count,
                       COALESCE(SUM(CASE WHEN review_status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejected_count,
                       COALESCE(SUM(CASE WHEN current_check_status = 'RESOLVED' THEN 1 ELSE 0 END), 0) AS resolved_count,
                       COALESCE(SUM(CASE WHEN current_check_status = 'STILL_ABNORMAL' THEN 1 ELSE 0 END), 0) AS still_abnormal_count
                FROM (
                """ + reviewSql(allowedProjectIds) + """
                ) r
                """, (rs, rowNum) -> new ReconcileReviewStatsView(
                rs.getLong("total_count"),
                rs.getLong("pending_count"),
                rs.getLong("approved_count"),
                rs.getLong("rejected_count"),
                rs.getLong("resolved_count"),
                rs.getLong("still_abnormal_count")), args.toArray());
    }

    public ReconcileExceptionReviewView getReconcileExceptionReview(Long tenantId, List<Long> allowedProjectIds,
                                                                    String exceptionKey) {
        List<Object> args = reviewArgs(tenantId, allowedProjectIds, null, null, null, null, null);
        args.add(exceptionKey);
        return jdbcTemplate.queryForObject("SELECT * FROM (" + reviewSql(allowedProjectIds)
                        + ") r WHERE r.exception_key = ?",
                this::mapReconcileExceptionReview, args.toArray());
    }

    public void upsertReconcileExceptionHandle(Long handleId, Long tenantId, Long projectId, String exceptionKey,
                                               String exceptionType, String businessType, Long businessId,
                                               Long userId, String remark, String attachmentFileIds,
                                               String reviewStatus) {
        jdbcTemplate.update("""
                        INSERT INTO payment_reconcile_exception_handle(handle_id, tenant_id, project_id, exception_key,
                                                                       exception_type, business_type, business_id,
                                                                       status, handle_remark, attachment_file_ids,
                                                                       handled_by, handled_at, review_status,
                                                                       reviewed_by, reviewed_at, review_remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'HANDLED', ?, ?, ?, NOW(), ?,
                                CASE WHEN ? = 'APPROVED' THEN ? ELSE NULL END,
                                CASE WHEN ? = 'APPROVED' THEN NOW() ELSE NULL END,
                                CASE WHEN ? = 'APPROVED' THEN '低风险异常处理后自动归档' ELSE NULL END)
                        ON DUPLICATE KEY UPDATE status = 'HANDLED',
                                                handle_remark = VALUES(handle_remark),
                                                attachment_file_ids = VALUES(attachment_file_ids),
                                                handled_by = VALUES(handled_by),
                                                handled_at = NOW(),
                                                review_status = VALUES(review_status),
                                                reviewed_by = CASE WHEN VALUES(review_status) = 'APPROVED' THEN VALUES(handled_by) ELSE NULL END,
                                                reviewed_at = CASE WHEN VALUES(review_status) = 'APPROVED' THEN NOW() ELSE NULL END,
                                                review_remark = CASE WHEN VALUES(review_status) = 'APPROVED' THEN '低风险异常处理后自动归档' ELSE NULL END,
                                                deleted = 0
                        """, handleId, tenantId, projectId, exceptionKey, exceptionType, businessType,
                businessId, remark, attachmentFileIds, userId, reviewStatus,
                reviewStatus, userId, reviewStatus, reviewStatus);
    }

    public void reviewReconcileExceptionHandle(Long tenantId, String exceptionKey, String reviewStatus, Long userId,
                                               String reviewRemark) {
        String status = "REJECTED".equals(reviewStatus) ? "OPEN" : "HANDLED";
        jdbcTemplate.update("""
                        UPDATE payment_reconcile_exception_handle
                        SET status = ?,
                            review_status = ?,
                            reviewed_by = ?,
                            reviewed_at = NOW(),
                            review_remark = ?,
                            updated_at = NOW()
                        WHERE tenant_id = ? AND exception_key = ? AND deleted = 0
                        """, status, reviewStatus, userId, reviewRemark, tenantId, exceptionKey);
    }

    public void insertReconcileExceptionHistory(Long historyId, Long tenantId, Long projectId, String exceptionKey,
                                                String actionType, String beforeStatus, String afterStatus,
                                                String beforeReviewStatus, String afterReviewStatus, String remark,
                                                String attachmentFileIds, Long operatorId) {
        jdbcTemplate.update("""
                        INSERT INTO payment_reconcile_exception_history(history_id, tenant_id, project_id, exception_key,
                                                                        action_type, before_status, after_status,
                                                                        before_review_status, after_review_status,
                                                                        remark, attachment_file_ids, operator_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, historyId, tenantId, projectId, exceptionKey, actionType, beforeStatus, afterStatus,
                beforeReviewStatus, afterReviewStatus, remark, attachmentFileIds, operatorId);
    }

    public List<ReconcileExceptionHistoryView> findReconcileExceptionHistory(Long tenantId, String exceptionKey,
                                                                             long offset, long pageSize) {
        return jdbcTemplate.query("""
                        SELECT history_id, exception_key, action_type, before_status, after_status,
                               before_review_status, after_review_status, remark, attachment_file_ids,
                               operator_id, created_at
                        FROM payment_reconcile_exception_history
                        WHERE tenant_id = ? AND exception_key = ? AND deleted = 0
                        ORDER BY created_at DESC, history_id DESC
                        LIMIT ? OFFSET ?
                        """, this::mapReconcileExceptionHistory, tenantId, exceptionKey, pageSize, offset);
    }

    public long countReconcileExceptionHistory(Long tenantId, String exceptionKey) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM payment_reconcile_exception_history
                        WHERE tenant_id = ? AND exception_key = ? AND deleted = 0
                        """, Long.class, tenantId, exceptionKey);
        return value(count);
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

    private void appendRefundFilters(StringBuilder sql, List<Object> args, Long projectId, String refundNo,
                                     String orderNo, String memberName, String status) {
        if (projectId != null) {
            sql.append(" AND r.project_id = ?");
            args.add(projectId);
        }
        if (refundNo != null && !refundNo.isBlank()) {
            sql.append(" AND r.refund_no LIKE ?");
            args.add("%" + refundNo + "%");
        }
        if (orderNo != null && !orderNo.isBlank()) {
            sql.append(" AND o.order_no LIKE ?");
            args.add("%" + orderNo + "%");
        }
        if (memberName != null && !memberName.isBlank()) {
            sql.append(" AND (m.real_name LIKE ? OR m.mobile LIKE ?)");
            args.add("%" + memberName + "%");
            args.add("%" + memberName + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ?");
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

    private PayRefundView mapRefund(ResultSet rs, int rowNum) throws SQLException {
        return new PayRefundView(rs.getLong("refund_id"), rs.getLong("project_id"), rs.getString("refund_no"),
                rs.getLong("order_id"), rs.getString("order_no"), (Long) rs.getObject("transaction_id"), rs.getBigDecimal("refund_amount"),
                rs.getString("reason"), rs.getString("status"), rs.getString("third_refund_no"),
                toLocalDateTime(rs, "refunded_at"), (Long) rs.getObject("apply_user_id"),
                (Long) rs.getObject("audit_user_id"), toLocalDateTime(rs, "audit_at"),
                rs.getTimestamp("created_at").toLocalDateTime(), (Long) rs.getObject("member_id"),
                rs.getString("member_name"), rs.getString("member_mobile"), rs.getString("house_no"),
                rs.getString("bill_summary"), rs.getString("pay_channel"), rs.getBigDecimal("order_amount"),
                rs.getBigDecimal("refunded_transaction_amount"));
    }

    private PayOrderView mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new PayOrderView(rs.getLong("order_id"), rs.getLong("tenant_id"), rs.getLong("project_id"),
                rs.getString("order_no"), (Long) rs.getObject("member_id"), rs.getString("pay_channel"),
                rs.getBigDecimal("amount"), rs.getString("subject"), rs.getString("status"), toLocalDateTime(rs, "expire_at"),
                toLocalDateTime(rs, "paid_at"), rs.getString("third_trade_no"),
                rs.getTimestamp("created_at").toLocalDateTime(), null, null, null, null, 0L,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
    }

    private RefundableOrderView mapRefundableOrder(ResultSet rs, int rowNum) throws SQLException {
        return new RefundableOrderView(rs.getLong("order_id"), rs.getString("order_no"), rs.getLong("project_id"),
                (Long) rs.getObject("member_id"), rs.getString("member_name"), rs.getString("mobile"),
                rs.getString("house_no"), rs.getBigDecimal("amount"), rs.getBigDecimal("refunded_amount"),
                rs.getBigDecimal("refundable_amount"), rs.getString("bill_summary"), toLocalDateTime(rs, "paid_at"));
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

    private record SummaryNumber(BigDecimal amount, long total) {
    }

    private SummaryNumber summary(String baseSql, Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                  String startDate, String endDate, String payChannel, String orderStatus,
                                  String projectColumn, String dateColumn, String payChannelColumn,
                                  String orderStatusColumn) {
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        StringBuilder sql = new StringBuilder(baseSql);
        appendReconcileFilters(sql, args, allowedProjectIds, projectId, startDate, endDate, payChannel,
                orderStatus, projectColumn, dateColumn, payChannelColumn, orderStatusColumn);
        return jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) ->
                new SummaryNumber(rs.getBigDecimal("amount"), rs.getLong("total")), args.toArray());
    }

    private void appendReconcileFilters(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds,
                                        Long projectId, String startDate, String endDate, String payChannel,
                                        String orderStatus, String projectColumn, String dateColumn,
                                        String payChannelColumn, String orderStatusColumn) {
        if (projectId != null) {
            sql.append(" AND ").append(projectColumn).append(" = ?");
            args.add(projectId);
        }
        if (startDate != null && !startDate.isBlank() && dateColumn != null) {
            sql.append(" AND ").append(dateColumn).append(" >= ?");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank() && dateColumn != null) {
            sql.append(" AND ").append(dateColumn).append(" <= ?");
            args.add(endDate + " 23:59:59");
        }
        if (payChannel != null && !payChannel.isBlank() && payChannelColumn != null) {
            sql.append(" AND ").append(payChannelColumn).append(" = ?");
            args.add(payChannel);
        }
        if (orderStatus != null && !orderStatus.isBlank() && orderStatusColumn != null) {
            sql.append(" AND ").append(orderStatusColumn).append(" = ?");
            args.add(orderStatus);
        }
        appendProjectScope(sql, args, allowedProjectIds, projectColumn);
    }

    private String exceptionSql(List<Long> allowedProjectIds) {
        String scope = allowedProjectIds == null ? "" : allowedProjectIds.isEmpty()
                ? " AND 1 = 0"
                : " AND e.project_id IN (" + "?,".repeat(allowedProjectIds.size()).replaceFirst(",$", "") + ")";
        return """
                SELECT e.*, COALESCE(h.status, 'OPEN') AS handle_status, h.handled_at, h.handled_by, h.handle_remark,
                       h.attachment_file_ids, COALESCE(h.review_status, 'NONE') AS review_status,
                       h.reviewed_at, h.reviewed_by, h.review_remark
                FROM (
                    SELECT CONCAT('ORDER_PAID_WITHOUT_TRANSACTION:', o.order_id) AS exception_key,
                           o.project_id,
                           '订单缺少支付流水' AS exception_type,
                           '高' AS exception_level,
                           '支付订单' AS business_type,
                           o.order_id AS business_id,
                           o.order_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           o.amount,
                           '订单已支付/退款，但没有对应支付流水，账实可能不一致' AS reason,
                           o.created_at
                    FROM pay_order o
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS paid_amount
                        FROM pay_transaction
                        GROUP BY tenant_id, order_id
                    ) tx ON tx.tenant_id = o.tenant_id AND tx.order_id = o.order_id
                    WHERE o.tenant_id = ? AND o.deleted = 0
                      AND o.status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                      AND COALESCE(tx.paid_amount, 0) = 0
                    UNION ALL
                    SELECT CONCAT('TRANSACTION_WITH_INVALID_ORDER:', t.transaction_id) AS exception_key,
                           t.project_id,
                           '支付流水订单异常' AS exception_type,
                           '高' AS exception_level,
                           '支付流水' AS business_type,
                           t.transaction_id AS business_id,
                           t.order_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           t.amount,
                           '支付流水找不到有效已支付订单，需核对订单状态或重复流水' AS reason,
                           t.created_at
                    FROM pay_transaction t
                    LEFT JOIN pay_order o ON o.tenant_id = t.tenant_id AND o.order_id = t.order_id AND o.deleted = 0
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    WHERE t.tenant_id = ?
                      AND (o.order_id IS NULL OR o.status NOT IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED'))
                    UNION ALL
                    SELECT CONCAT('REFUND_WITHOUT_TRANSACTION:', r.refund_id) AS exception_key,
                           r.project_id,
                           '退款缺少退款流水' AS exception_type,
                           '高' AS exception_level,
                           '退款单' AS business_type,
                           r.refund_id AS business_id,
                           r.refund_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           r.refund_amount AS amount,
                           '退款单已退款，但没有对应退款流水' AS reason,
                           r.created_at
                    FROM pay_refund r
                    LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, refund_id, SUM(refund_amount) AS refund_tx_amount
                        FROM pay_refund_transaction
                        GROUP BY tenant_id, refund_id
                    ) rt ON rt.tenant_id = r.tenant_id AND rt.refund_id = r.refund_id
                    WHERE r.tenant_id = ? AND r.deleted = 0
                      AND r.status = 'REFUNDED'
                      AND COALESCE(rt.refund_tx_amount, 0) = 0
                    UNION ALL
                    SELECT CONCAT('ORDER_AMOUNT_MISMATCH:', o.order_id) AS exception_key,
                           o.project_id,
                           '订单金额不一致' AS exception_type,
                           '中' AS exception_level,
                           '支付订单' AS business_type,
                           o.order_id AS business_id,
                           o.order_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           ABS(o.amount - (COALESCE(ob.bill_amount, 0) + COALESCE(pp.prepayment_amount, 0))) AS amount,
                           '订单金额与账单核销金额加预存款金额不一致' AS reason,
                           o.created_at
                    FROM pay_order o
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS bill_amount
                        FROM pay_order_bill
                        GROUP BY tenant_id, order_id
                    ) ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount
                        FROM member_prepayment
                        WHERE deleted = 0
                        GROUP BY tenant_id, order_id
                    ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                    WHERE o.tenant_id = ? AND o.deleted = 0
                      AND o.status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                      AND ABS(o.amount - (COALESCE(ob.bill_amount, 0) + COALESCE(pp.prepayment_amount, 0))) >= 0.01
                    UNION ALL
                    SELECT CONCAT('ORDER_PAID_ALLOCATION_MISMATCH:', o.order_id) AS exception_key,
                           o.project_id,
                           '订单实收核销不平' AS exception_type,
                           '高' AS exception_level,
                           '支付订单' AS business_type,
                           o.order_id AS business_id,
                           o.order_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           ABS(COALESCE(tx.paid_amount, 0) - COALESCE(ob.bill_amount, 0) - COALESCE(pp.prepayment_amount, 0)) AS amount,
                           '实收金额与账单核销金额加预存款转入金额不一致' AS reason,
                           o.created_at
                    FROM pay_order o
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS paid_amount
                        FROM pay_transaction
                        GROUP BY tenant_id, order_id
                    ) tx ON tx.tenant_id = o.tenant_id AND tx.order_id = o.order_id
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS bill_amount
                        FROM pay_order_bill
                        GROUP BY tenant_id, order_id
                    ) ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                    LEFT JOIN (
                        SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount
                        FROM member_prepayment
                        WHERE deleted = 0
                        GROUP BY tenant_id, order_id
                    ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                    WHERE o.tenant_id = ? AND o.deleted = 0
                      AND o.status IN ('PAID', 'REFUNDING', 'REFUNDED', 'PARTIAL_REFUNDED')
                      AND COALESCE(tx.paid_amount, 0) > 0
                      AND ABS(COALESCE(tx.paid_amount, 0) - COALESCE(ob.bill_amount, 0) - COALESCE(pp.prepayment_amount, 0)) >= 0.01
                    UNION ALL
                    SELECT CONCAT('REFUND_AMOUNT_MISMATCH:', r.refund_id) AS exception_key,
                           r.project_id,
                           '退款流水金额不平' AS exception_type,
                           '高' AS exception_level,
                           '退款单' AS business_type,
                           r.refund_id AS business_id,
                           r.refund_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           ABS(r.refund_amount - COALESCE(rt.refund_tx_amount, 0)) AS amount,
                           '退款单金额与退款流水金额不一致' AS reason,
                           r.created_at
                    FROM pay_refund r
                    LEFT JOIN pay_order o ON o.tenant_id = r.tenant_id AND o.order_id = r.order_id AND o.deleted = 0
                    LEFT JOIN member_user m ON m.tenant_id = o.tenant_id AND m.member_id = o.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, refund_id, SUM(refund_amount) AS refund_tx_amount
                        FROM pay_refund_transaction
                        GROUP BY tenant_id, refund_id
                    ) rt ON rt.tenant_id = r.tenant_id AND rt.refund_id = r.refund_id
                    WHERE r.tenant_id = ? AND r.deleted = 0
                      AND r.status = 'REFUNDED'
                      AND COALESCE(rt.refund_tx_amount, 0) > 0
                      AND ABS(r.refund_amount - COALESCE(rt.refund_tx_amount, 0)) >= 0.01
                    UNION ALL
                    SELECT CONCAT('PREPAYMENT_BALANCE_MISMATCH:', p.prepayment_id) AS exception_key,
                           p.project_id,
                           '预存款余额异常' AS exception_type,
                           '高' AS exception_level,
                           '业主预存款' AS business_type,
                           p.prepayment_id AS business_id,
                           p.order_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           ABS(p.amount - p.remaining_amount - COALESCE(p.refunded_amount, 0) - COALESCE(u.used_amount, 0)) AS amount,
                           '预存款余额与已抵扣金额、已退款金额不一致，或余额超出合理范围' AS reason,
                           p.created_at
                    FROM member_prepayment p
                    LEFT JOIN member_user m ON m.tenant_id = p.tenant_id AND m.member_id = p.member_id AND m.deleted = 0
                    LEFT JOIN (
                        SELECT tenant_id, prepayment_id, SUM(amount) AS used_amount
                        FROM member_prepayment_usage
                        WHERE deleted = 0
                        GROUP BY tenant_id, prepayment_id
                    ) u ON u.tenant_id = p.tenant_id AND u.prepayment_id = p.prepayment_id
                    WHERE p.tenant_id = ? AND p.deleted = 0
                      AND (p.remaining_amount < 0
                           OR p.remaining_amount > p.amount
                           OR COALESCE(p.refunded_amount, 0) < 0
                           OR COALESCE(p.refunded_amount, 0) > p.amount
                           OR ABS(p.amount - p.remaining_amount - COALESCE(p.refunded_amount, 0) - COALESCE(u.used_amount, 0)) >= 0.01)
                    UNION ALL
                    SELECT CONCAT('BILL_AMOUNT_STATUS_MISMATCH:', b.bill_id) AS exception_key,
                           b.project_id,
                           '账单金额状态异常' AS exception_type,
                           '中' AS exception_level,
                           '收费账单' AS business_type,
                           b.bill_id AS business_id,
                           b.bill_no AS business_no,
                           m.real_name AS member_name,
                           m.mobile AS member_mobile,
                           b.remaining_amount AS amount,
                           '账单状态与待收金额不匹配，需核对收款/退款/作废记录' AS reason,
                           b.created_at
                    FROM fee_bill b
                    LEFT JOIN member_user m ON m.tenant_id = b.tenant_id AND m.member_id = b.member_id AND m.deleted = 0
                    WHERE b.tenant_id = ? AND b.deleted = 0
                      AND ((b.status = 'PAID' AND b.remaining_amount <> 0)
                           OR (b.status IN ('UNPAID', 'OVERDUE', 'PARTIAL_PAID') AND b.remaining_amount <= 0))
                ) e
                LEFT JOIN payment_reconcile_exception_handle h
                  ON h.tenant_id = ? AND h.exception_key = e.exception_key AND h.deleted = 0
                WHERE 1 = 1
                """ + scope + """
                AND (? IS NULL OR e.project_id = ?)
                AND (? IS NULL OR e.exception_type = ?)
                AND (? IS NULL OR e.exception_level = ?)
                AND (? IS NULL OR e.business_no LIKE ?)
                AND (? IS NULL OR e.member_name LIKE ? OR e.member_mobile LIKE ?)
                AND (? IS NULL OR COALESCE(h.status, 'OPEN') = ?)
                """;
    }

    private List<Object> exceptionArgs(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                       String exceptionType, String exceptionLevel, String businessNo,
                                       String memberName, String status) {
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        addExceptionSqlArgs(args, allowedProjectIds, projectId, normalizeBlank(exceptionType),
                normalizeBlank(exceptionLevel), normalizeLike(businessNo), normalizeLike(memberName),
                normalizeBlank(status));
        return args;
    }

    private String reviewSql(List<Long> allowedProjectIds) {
        String scope = "";
        if (allowedProjectIds != null && !allowedProjectIds.isEmpty()) {
            scope = " AND h.project_id IN (" + placeholders(allowedProjectIds.size()) + ") ";
        }
        return """
                SELECT h.exception_key,
                       h.project_id,
                       COALESCE(e.exception_type, h.exception_type) AS exception_type,
                       COALESCE(e.exception_level, '-') AS exception_level,
                       COALESCE(e.business_type, h.business_type) AS business_type,
                       COALESCE(e.business_id, h.business_id) AS business_id,
                       e.business_no,
                       e.member_name,
                       e.member_mobile,
                       COALESCE(e.amount, 0) AS amount,
                       CASE WHEN e.exception_key IS NULL THEN '处理后复算已无异常，可复核通过'
                            ELSE e.reason END AS reason,
                       h.status AS handle_status,
                       h.handled_at,
                       h.handled_by,
                       h.handle_remark,
                       h.attachment_file_ids,
                       h.review_status,
                       h.reviewed_at,
                       h.reviewed_by,
                       h.review_remark,
                       CASE WHEN e.exception_key IS NULL THEN 'RESOLVED'
                            ELSE 'STILL_ABNORMAL' END AS current_check_status,
                       h.created_at
                FROM payment_reconcile_exception_handle h
                LEFT JOIN (
                """ + exceptionSql(allowedProjectIds) + """
                ) e ON e.exception_key = h.exception_key
                WHERE h.tenant_id = ? AND h.deleted = 0
                AND h.status IN ('HANDLED', 'OPEN')
                AND h.review_status IN ('PENDING', 'APPROVED', 'REJECTED')
                """ + scope + """
                AND (? IS NULL OR h.project_id = ?)
                AND (? IS NULL OR COALESCE(e.exception_type, h.exception_type) = ?)
                AND (? IS NULL OR COALESCE(e.member_name, '') LIKE ? OR COALESCE(e.member_mobile, '') LIKE ?)
                AND (? IS NULL OR h.review_status = ?)
                AND (? IS NULL OR (CASE WHEN e.exception_key IS NULL THEN 'RESOLVED' ELSE 'STILL_ABNORMAL' END) = ?)
                """;
    }

    private List<Object> reviewArgs(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                    String exceptionType, String memberName, String reviewStatus,
                                    String currentCheckStatus) {
        List<Object> args = exceptionArgs(tenantId, allowedProjectIds, null, null, null, null, null, null);
        args.add(tenantId);
        if (allowedProjectIds != null && !allowedProjectIds.isEmpty()) {
            args.addAll(allowedProjectIds);
        }
        args.add(projectId);
        args.add(projectId);
        args.add(normalizeBlank(exceptionType));
        args.add(normalizeBlank(exceptionType));
        args.add(normalizeLike(memberName));
        args.add(normalizeLike(memberName));
        args.add(normalizeLike(memberName));
        args.add(normalizeBlank(reviewStatus));
        args.add(normalizeBlank(reviewStatus));
        args.add(normalizeBlank(currentCheckStatus));
        args.add(normalizeBlank(currentCheckStatus));
        return args;
    }

    private void addExceptionSqlArgs(List<Object> args, List<Long> allowedProjectIds, Long projectId,
                                     String exceptionType, String exceptionLevel, String businessNo,
                                     String memberName, String status) {
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        args.add(args.get(0));
        if (allowedProjectIds != null && !allowedProjectIds.isEmpty()) {
            args.addAll(allowedProjectIds);
        }
        if (projectId != null) {
            args.add(projectId);
        } else {
            args.add(null);
        }
        args.add(projectId);
        args.add(exceptionType);
        args.add(exceptionType);
        args.add(exceptionLevel);
        args.add(exceptionLevel);
        args.add(businessNo);
        args.add(businessNo);
        args.add(memberName);
        args.add(memberName);
        args.add(memberName);
        args.add(status);
        args.add(status);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeLike(String value) {
        return value == null || value.isBlank() ? null : "%" + value.trim() + "%";
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private ReconcileExceptionView mapReconcileException(ResultSet rs, int rowNum) throws SQLException {
        return new ReconcileExceptionView(rs.getString("exception_key"), rs.getLong("project_id"),
                rs.getString("exception_type"), rs.getString("exception_level"), rs.getString("business_type"),
                (Long) rs.getObject("business_id"), rs.getString("business_no"), rs.getString("member_name"),
                rs.getString("member_mobile"), rs.getBigDecimal("amount"), rs.getString("reason"),
                rs.getString("handle_status"), toLocalDateTime(rs, "handled_at"), (Long) rs.getObject("handled_by"),
                rs.getString("handle_remark"), rs.getString("attachment_file_ids"), rs.getString("review_status"),
                toLocalDateTime(rs, "reviewed_at"), (Long) rs.getObject("reviewed_by"), rs.getString("review_remark"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private ReconcileExceptionReviewView mapReconcileExceptionReview(ResultSet rs, int rowNum) throws SQLException {
        return new ReconcileExceptionReviewView(rs.getString("exception_key"), rs.getLong("project_id"),
                rs.getString("exception_type"), rs.getString("exception_level"), rs.getString("business_type"),
                (Long) rs.getObject("business_id"), rs.getString("business_no"), rs.getString("member_name"),
                rs.getString("member_mobile"), rs.getBigDecimal("amount"), rs.getString("reason"),
                rs.getString("handle_status"), toLocalDateTime(rs, "handled_at"), (Long) rs.getObject("handled_by"),
                rs.getString("handle_remark"), rs.getString("attachment_file_ids"), rs.getString("review_status"),
                toLocalDateTime(rs, "reviewed_at"), (Long) rs.getObject("reviewed_by"), rs.getString("review_remark"),
                rs.getString("current_check_status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private ReconcileExceptionHistoryView mapReconcileExceptionHistory(ResultSet rs, int rowNum) throws SQLException {
        return new ReconcileExceptionHistoryView(
                rs.getLong("history_id"),
                rs.getString("exception_key"),
                rs.getString("action_type"),
                rs.getString("before_status"),
                rs.getString("after_status"),
                rs.getString("before_review_status"),
                rs.getString("after_review_status"),
                rs.getString("remark"),
                rs.getString("attachment_file_ids"),
                (Long) rs.getObject("operator_id"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private record PrepaymentForRefund(Long prepaymentId, BigDecimal remainingAmount) {
    }
}
