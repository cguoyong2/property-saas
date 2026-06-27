package com.yongquan.propertysaas.app.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findHouses(Long tenantId, Long memberId) {
        return jdbcTemplate.queryForList("""
                SELECT b.bind_id AS bindId, b.project_id AS projectId, p.project_name AS projectName,
                       b.house_id AS houseId, h.house_no AS houseNo, h.building_id AS buildingId,
                       bd.building_name AS buildingName, h.unit_id AS unitId, u.unit_name AS unitName,
                       CONCAT_WS('', bd.building_name, u.unit_name, h.house_no) AS roomNo,
                       h.building_area AS buildingArea, h.house_status AS houseStatus,
                       b.bind_role AS bindRole, b.relationship, b.invite_member_id AS inviteMemberId,
                       b.allow_notice AS allowNotice, b.allow_bill AS allowBill,
                       b.allow_payment AS allowPayment, b.allow_work_order AS allowWorkOrder,
                       b.allow_visitor AS allowVisitor,
                       CASE WHEN b.invite_member_id IS NOT NULL THEN '业主邀请'
                            WHEN b.created_by = b.member_id THEN '业主端提交'
                            ELSE '物业端提交' END AS applySource,
                       COALESCE(NULLIF(b.status, ''), 'PENDING') AS status,
                       b.effective_date AS effectiveDate, b.expire_date AS expireDate,
                       b.audit_at AS auditAt, b.audit_remark AS auditRemark,
                       b.created_at AS createdAt
                FROM member_house_bind b
                JOIN base_project p ON p.tenant_id = b.tenant_id AND p.project_id = b.project_id AND p.deleted = 0
                JOIN base_house h ON h.tenant_id = b.tenant_id AND h.project_id = b.project_id
                                 AND h.house_id = b.house_id AND h.deleted = 0
                LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                WHERE b.tenant_id = ? AND b.member_id = ? AND b.deleted = 0
                ORDER BY COALESCE(NULLIF(b.status, ''), 'PENDING') = 'APPROVED' DESC,
                         COALESCE(NULLIF(b.status, ''), 'PENDING') = 'PENDING' DESC,
                         b.created_at DESC
                """, tenantId, memberId);
    }

    public List<Map<String, Object>> findBindProjects(Long tenantId) {
        return jdbcTemplate.queryForList("""
                SELECT project_id AS projectId, project_name AS projectName, city, district, address
                FROM base_project
                WHERE tenant_id = ? AND deleted = 0 AND status = 'ACTIVE'
                ORDER BY created_at DESC, project_id DESC
                """, tenantId);
    }

    public List<Map<String, Object>> findBindBuildings(Long tenantId, Long projectId) {
        return jdbcTemplate.queryForList("""
                SELECT building_id AS buildingId, project_id AS projectId, building_name AS buildingName, floor_count AS floorCount
                FROM base_building
                WHERE tenant_id = ? AND project_id = ? AND deleted = 0 AND status = 'ACTIVE'
                ORDER BY sort_no ASC, building_id ASC
                """, tenantId, projectId);
    }

    public List<Map<String, Object>> findBindUnits(Long tenantId, Long projectId, Long buildingId) {
        return jdbcTemplate.queryForList("""
                SELECT unit_id AS unitId, project_id AS projectId, building_id AS buildingId, unit_name AS unitName
                FROM base_unit
                WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND deleted = 0 AND status = 'ACTIVE'
                ORDER BY sort_no ASC, unit_id ASC
                """, tenantId, projectId, buildingId);
    }

    public List<Map<String, Object>> findBindHouses(Long tenantId, Long projectId, Long buildingId, Long unitId) {
        return jdbcTemplate.queryForList("""
                SELECT house_id AS houseId, project_id AS projectId, building_id AS buildingId, unit_id AS unitId,
                       house_no AS houseNo, floor_no AS floorNo, building_area AS buildingArea,
                       house_usage AS houseUsage, house_status AS houseStatus
                FROM base_house
                WHERE tenant_id = ? AND project_id = ? AND building_id = ? AND unit_id = ? AND deleted = 0
                ORDER BY floor_no ASC, house_no ASC, house_id ASC
                """, tenantId, projectId, buildingId, unitId);
    }

    public List<Map<String, Object>> findBills(Long tenantId, Long memberId, Long houseId, String status,
                                               long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(billSelectSql());
        appendBillAccessFilter(sql, args, tenantId, memberId, houseId);
        appendBillStatusFilter(sql, args, status);
        sql.append(" ORDER BY fb.due_date IS NULL, fb.due_date ASC, fb.created_at DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public long countBills(Long tenantId, Long memberId, Long houseId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fee_bill fb ");
        appendBillAccessFilter(sql, args, tenantId, memberId, houseId);
        appendBillStatusFilter(sql, args, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public Map<String, Object> getBill(Long tenantId, Long memberId, Long billId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(billSelectSql());
        sql.append(" WHERE fb.tenant_id = ? AND fb.bill_id = ? AND fb.deleted = 0");
        args.add(tenantId);
        args.add(billId);
        appendMemberBillAccessFilter(sql, args, memberId, "allow_bill");
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public long countAccessiblePayableBills(Long tenantId, Long memberId, Long projectId, List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return 0;
        }
        List<Long> distinctBillIds = billIds.stream().distinct().toList();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM fee_bill fb
                WHERE fb.tenant_id = ? AND fb.project_id = ? AND fb.deleted = 0
                  AND fb.status IN ('UNPAID', 'OVERDUE', 'PARTIAL_PAID')
                  AND fb.bill_id IN (
                """);
        sql.append("?,".repeat(distinctBillIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(projectId);
        args.addAll(distinctBillIds);
        appendMemberBillAccessFilter(sql, args, memberId, "allow_payment");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> findPayOrders(Long tenantId, Long memberId, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(payOrderSelectSql());
        appendPayOrderFilter(sql, args, tenantId, memberId, status);
        sql.append(" ORDER BY o.created_at DESC, o.order_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public long countPayOrders(Long tenantId, Long memberId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM pay_order o
                """);
        appendPayOrderFilter(sql, args, tenantId, memberId, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public Map<String, Object> getPayOrder(Long tenantId, Long memberId, String orderNo) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(payOrderSelectSql());
        appendPayOrderFilter(sql, args, tenantId, memberId, null);
        sql.append(" AND o.order_no = ?");
        args.add(orderNo);
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public List<Map<String, Object>> findPrepaymentLedger(Long tenantId, Long memberId, long offset, long pageSize) {
        return jdbcTemplate.queryForList("""
                SELECT *
                FROM (
                  SELECT p.created_at AS createdAt, p.project_id AS projectId, bp.project_name AS projectName,
                         p.order_no AS businessNo, 'IN' AS direction, p.source AS source,
                         p.amount AS amount, p.remaining_amount AS remainingAmount,
                         COALESCE(p.refunded_amount, 0) AS refundedAmount,
                         NULL AS billNo, NULL AS feeItemName, NULL AS billPeriod,
                         ob.house_no AS houseNo, ob.bill_summary AS billSummary, p.order_no AS orderNo,
                         p.remark AS remark
                  FROM member_prepayment p
                  LEFT JOIN base_project bp ON bp.tenant_id = p.tenant_id
                      AND bp.project_id = p.project_id AND bp.deleted = 0
                  LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id,
                           GROUP_CONCAT(DISTINCT CONCAT_WS('', bd.building_name, bu.unit_name, h.house_no)
                                        ORDER BY bd.building_name, bu.unit_name, h.house_no SEPARATOR '、') AS house_no,
                           GROUP_CONCAT(DISTINCT CONCAT(COALESCE(fi.item_name, '费用'), ' ', fb.bill_period, ' ', ob.amount, '元')
                                        ORDER BY fb.bill_period, fi.item_name SEPARATOR '；') AS bill_summary
                    FROM pay_order_bill ob
                    LEFT JOIN fee_bill fb ON fb.tenant_id = ob.tenant_id AND fb.bill_id = ob.bill_id AND fb.deleted = 0
                    LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                    LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                    LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                    LEFT JOIN base_unit bu ON bu.tenant_id = h.tenant_id AND bu.unit_id = h.unit_id AND bu.deleted = 0
                    GROUP BY ob.tenant_id, ob.order_id
                  ) ob ON ob.tenant_id = p.tenant_id AND ob.order_id = p.order_id
                  WHERE p.tenant_id = ? AND p.member_id = ? AND p.deleted = 0
                  UNION ALL
                  SELECT u.created_at AS createdAt, u.project_id AS projectId, bp.project_name AS projectName,
                         u.bill_no AS businessNo, 'OUT' AS direction, u.usage_type AS source,
                         u.amount AS amount, NULL AS remainingAmount, NULL AS refundedAmount,
                         fb.bill_no AS billNo, fi.item_name AS feeItemName, fb.bill_period AS billPeriod,
                         CONCAT_WS('', bd.building_name, bu.unit_name, h.house_no) AS houseNo,
                         CONCAT(COALESCE(fi.item_name, '费用'), ' ', fb.bill_period, ' ', u.amount, '元') AS billSummary,
                         NULL AS orderNo,
                         u.remark AS remark
                  FROM member_prepayment_usage u
                  LEFT JOIN base_project bp ON bp.tenant_id = u.tenant_id
                      AND bp.project_id = u.project_id AND bp.deleted = 0
                  LEFT JOIN fee_bill fb ON fb.tenant_id = u.tenant_id AND fb.bill_id = u.bill_id AND fb.deleted = 0
                  LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                  LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                  LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                  LEFT JOIN base_unit bu ON bu.tenant_id = h.tenant_id AND bu.unit_id = h.unit_id AND bu.deleted = 0
                  WHERE u.tenant_id = ? AND u.member_id = ? AND u.deleted = 0
                ) t
                ORDER BY t.createdAt DESC
                LIMIT ? OFFSET ?
                """, tenantId, memberId, tenantId, memberId, pageSize, offset);
    }

    public long countPrepaymentLedger(Long tenantId, Long memberId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM member_prepayment
                   WHERE tenant_id = ? AND member_id = ? AND deleted = 0)
                  +
                  (SELECT COUNT(*) FROM member_prepayment_usage
                   WHERE tenant_id = ? AND member_id = ? AND deleted = 0)
                """, Long.class, tenantId, memberId, tenantId, memberId);
        return count == null ? 0 : count;
    }

    public Map<String, Object> prepaymentSummary(Long tenantId, Long memberId) {
        return jdbcTemplate.queryForMap("""
                SELECT
                  COALESCE((SELECT SUM(amount) FROM member_prepayment
                            WHERE tenant_id = ? AND member_id = ? AND deleted = 0), 0) AS totalAmount,
                  COALESCE((SELECT SUM(amount - remaining_amount - COALESCE(refunded_amount, 0)) FROM member_prepayment
                            WHERE tenant_id = ? AND member_id = ? AND deleted = 0), 0) AS usedAmount,
                  COALESCE((SELECT SUM(COALESCE(refunded_amount, 0)) FROM member_prepayment
                            WHERE tenant_id = ? AND member_id = ? AND deleted = 0), 0) AS refundedAmount,
                  COALESCE((SELECT SUM(remaining_amount) FROM member_prepayment
                            WHERE tenant_id = ? AND member_id = ? AND deleted = 0), 0) AS remainingAmount
                """, tenantId, memberId, tenantId, memberId, tenantId, memberId, tenantId, memberId);
    }

    public List<Map<String, Object>> findVehicles(Long tenantId, Long memberId, long offset, long pageSize) {
        return jdbcTemplate.queryForList("""
                SELECT vehicle_id AS vehicleId, project_id AS projectId, plate_no AS plateNo,
                       vehicle_type AS vehicleType, member_id AS memberId, house_id AS houseId, space_id AS spaceId,
                       monthly_rent_status AS monthlyRentStatus, start_date AS startDate, end_date AS endDate,
                       status, created_at AS createdAt
                FROM base_vehicle
                WHERE tenant_id = ? AND member_id = ? AND deleted = 0
                ORDER BY created_at DESC, vehicle_id DESC
                LIMIT ? OFFSET ?
                """, tenantId, memberId, pageSize, offset);
    }

    public long countVehicles(Long tenantId, Long memberId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM base_vehicle
                WHERE tenant_id = ? AND member_id = ? AND deleted = 0
                """, Long.class, tenantId, memberId);
        return count == null ? 0 : count;
    }

    public Map<String, Object> getMember(Long tenantId, Long memberId) {
        return jdbcTemplate.queryForMap("""
                SELECT member_id AS memberId, openid, mobile, real_name AS realName, avatar_url AS avatarUrl,
                       status, last_login_at AS lastLoginAt, created_at AS createdAt
                FROM member_user
                WHERE tenant_id = ? AND member_id = ? AND deleted = 0
                """, tenantId, memberId);
    }

    public Map<String, Object> homeSummary(Long tenantId, Long memberId) {
        return jdbcTemplate.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM member_house_bind WHERE tenant_id = ? AND member_id = ? AND status = 'APPROVED' AND deleted = 0) AS houseCount,
                  (SELECT COUNT(*) FROM fee_bill fb WHERE fb.tenant_id = ? AND fb.deleted = 0 AND fb.status IN ('UNPAID','OVERDUE','PARTIAL_PAID')
                    AND EXISTS (SELECT 1 FROM member_house_bind b WHERE b.tenant_id = fb.tenant_id
                      AND b.project_id = fb.project_id AND b.member_id = ? AND b.status = 'APPROVED'
                      AND (b.bind_role = 'OWNER' OR b.allow_bill = 1) AND b.deleted = 0
                      AND (fb.house_id = b.house_id
                        OR (fb.object_type = 'HOUSE' AND fb.object_id = b.house_id)
                        OR EXISTS (SELECT 1 FROM base_parking_space s WHERE s.tenant_id = fb.tenant_id
                          AND s.project_id = fb.project_id AND s.space_id = fb.object_id
                          AND fb.object_type = 'SPACE' AND s.house_id = b.house_id AND s.deleted = 0)
                        OR EXISTS (SELECT 1 FROM base_vehicle v WHERE v.tenant_id = fb.tenant_id
                          AND v.project_id = fb.project_id AND v.vehicle_id = fb.object_id
                          AND fb.object_type = 'VEHICLE' AND v.member_id = b.member_id AND v.deleted = 0)
                        OR fb.member_id = b.member_id))) AS unpaidBillCount,
                  (SELECT COALESCE(SUM(fb.remaining_amount), 0) FROM fee_bill fb WHERE fb.tenant_id = ? AND fb.deleted = 0 AND fb.status IN ('UNPAID','OVERDUE','PARTIAL_PAID')
                    AND EXISTS (SELECT 1 FROM member_house_bind b WHERE b.tenant_id = fb.tenant_id
                      AND b.project_id = fb.project_id AND b.member_id = ? AND b.status = 'APPROVED'
                      AND (b.bind_role = 'OWNER' OR b.allow_bill = 1) AND b.deleted = 0
                      AND (fb.house_id = b.house_id
                        OR (fb.object_type = 'HOUSE' AND fb.object_id = b.house_id)
                        OR EXISTS (SELECT 1 FROM base_parking_space s WHERE s.tenant_id = fb.tenant_id
                          AND s.project_id = fb.project_id AND s.space_id = fb.object_id
                          AND fb.object_type = 'SPACE' AND s.house_id = b.house_id AND s.deleted = 0)
                        OR EXISTS (SELECT 1 FROM base_vehicle v WHERE v.tenant_id = fb.tenant_id
                          AND v.project_id = fb.project_id AND v.vehicle_id = fb.object_id
                          AND fb.object_type = 'VEHICLE' AND v.member_id = b.member_id AND v.deleted = 0)
                        OR fb.member_id = b.member_id))) AS unpaidAmount,
                  (SELECT COUNT(*) FROM work_order WHERE tenant_id = ? AND member_id = ? AND deleted = 0) AS workOrderCount,
                  (SELECT COUNT(*) FROM base_vehicle WHERE tenant_id = ? AND member_id = ? AND deleted = 0) AS vehicleCount
                """, tenantId, memberId, tenantId, memberId, tenantId, memberId, tenantId, memberId, tenantId, memberId);
    }

    public boolean approvedHouseExists(Long tenantId, Long memberId, Long houseId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM member_house_bind
                WHERE tenant_id = ? AND member_id = ? AND house_id = ? AND status = 'APPROVED' AND deleted = 0
                """, Integer.class, tenantId, memberId, houseId);
        return count != null && count > 0;
    }

    private String billSelectSql() {
        return """
                SELECT fb.bill_id AS billId, fb.project_id AS projectId, p.project_name AS projectName,
                       fb.bill_no AS billNo, fb.item_id AS itemId, fi.item_name AS feeItemName,
                       fb.standard_id AS standardId, fb.object_type AS objectType, fb.object_id AS objectId,
                       fb.member_id AS memberId, fb.house_id AS houseId,
                       CONCAT_WS('', bd.building_name, u.unit_name, h.house_no) AS houseNo,
                       bd.building_name AS buildingName, u.unit_name AS unitName, h.house_no AS roomNo,
                       fb.bill_period AS billPeriod, fb.receivable_amount AS receivableAmount,
                       fb.discount_amount AS discountAmount, fb.paid_amount AS paidAmount,
                       fb.refund_amount AS refundAmount, COALESCE(pu.prepayment_applied_amount, 0) AS prepaymentAppliedAmount,
                       fb.remaining_amount AS remainingAmount, fb.due_date AS dueDate, fb.status,
                       fb.source_type AS sourceType, fb.created_at AS createdAt,
                       COALESCE(po.payment_summary, '') AS paymentSummary,
                       COALESCE(rf.refund_summary, '') AS refundSummary
                FROM fee_bill fb
                LEFT JOIN base_project p ON p.tenant_id = fb.tenant_id AND p.project_id = fb.project_id AND p.deleted = 0
                LEFT JOIN fee_item fi ON fi.tenant_id = fb.tenant_id AND fi.item_id = fb.item_id AND fi.deleted = 0
                LEFT JOIN base_house h ON h.tenant_id = fb.tenant_id AND h.house_id = fb.house_id AND h.deleted = 0
                LEFT JOIN base_building bd ON bd.tenant_id = h.tenant_id AND bd.building_id = h.building_id AND bd.deleted = 0
                LEFT JOIN base_unit u ON u.tenant_id = h.tenant_id AND u.unit_id = h.unit_id AND u.deleted = 0
                LEFT JOIN (
                    SELECT tenant_id, bill_id, SUM(amount) AS prepayment_applied_amount
                    FROM member_prepayment_usage
                    WHERE deleted = 0
                    GROUP BY tenant_id, bill_id
                ) pu ON pu.tenant_id = fb.tenant_id AND pu.bill_id = fb.bill_id
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.bill_id,
                           GROUP_CONCAT(DISTINCT CONCAT(o.pay_channel, ' ', o.status, ' ', ob.amount, '元')
                                        ORDER BY o.created_at DESC SEPARATOR '；') AS payment_summary
                    FROM pay_order_bill ob
                    JOIN pay_order o ON o.tenant_id = ob.tenant_id AND o.order_id = ob.order_id AND o.deleted = 0
                    GROUP BY ob.tenant_id, ob.bill_id
                ) po ON po.tenant_id = fb.tenant_id AND po.bill_id = fb.bill_id
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.bill_id,
                           GROUP_CONCAT(DISTINCT CONCAT(r.status, ' ', r.refund_amount, '元')
                                        ORDER BY r.created_at DESC SEPARATOR '；') AS refund_summary
                    FROM pay_order_bill ob
                    JOIN pay_refund r ON r.tenant_id = ob.tenant_id AND r.order_id = ob.order_id AND r.deleted = 0
                    GROUP BY ob.tenant_id, ob.bill_id
                ) rf ON rf.tenant_id = fb.tenant_id AND rf.bill_id = fb.bill_id
                """;
    }

    private String payOrderSelectSql() {
        return """
                SELECT o.order_id AS orderId, o.project_id AS projectId, bp.project_name AS projectName,
                       o.order_no AS orderNo, o.pay_channel AS payChannel, o.amount, o.subject, o.status,
                       o.expire_at AS expireAt, o.paid_at AS paidAt, o.third_trade_no AS thirdTradeNo,
                       o.created_at AS createdAt,
                       COALESCE(ob.bill_count, 0) AS billCount,
                       COALESCE(ob.bill_applied_amount, 0) AS billAppliedAmount,
                       COALESCE(tx.transaction_amount, 0) AS transactionAmount,
                       COALESCE(r.refunded_amount, 0) AS refundedAmount,
                       COALESCE(pp.prepayment_amount, 0) AS prepaymentAmount,
                       COALESCE(pp.prepayment_remaining_amount, 0) AS prepaymentRemainingAmount,
                       ob.house_no AS houseNo,
                       ob.bill_summary AS billSummary
                FROM pay_order o
                LEFT JOIN base_project bp ON bp.tenant_id = o.tenant_id
                    AND bp.project_id = o.project_id AND bp.deleted = 0
                LEFT JOIN (
                    SELECT ob.tenant_id, ob.order_id, COUNT(*) AS bill_count, SUM(ob.amount) AS bill_applied_amount,
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
                ) ob ON ob.tenant_id = o.tenant_id AND ob.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS transaction_amount
                    FROM pay_transaction
                    GROUP BY tenant_id, order_id
                ) tx ON tx.tenant_id = o.tenant_id AND tx.order_id = o.order_id
                LEFT JOIN (
                    SELECT rt.tenant_id, r.order_id, SUM(rt.refund_amount) AS refunded_amount
                    FROM pay_refund_transaction rt
                    JOIN pay_refund r ON r.tenant_id = rt.tenant_id AND r.refund_id = rt.refund_id AND r.deleted = 0
                    GROUP BY rt.tenant_id, r.order_id
                ) r ON r.tenant_id = o.tenant_id AND r.order_id = o.order_id
                LEFT JOIN (
                    SELECT tenant_id, order_id, SUM(amount) AS prepayment_amount,
                           SUM(remaining_amount) AS prepayment_remaining_amount
                    FROM member_prepayment
                    WHERE deleted = 0
                    GROUP BY tenant_id, order_id
                ) pp ON pp.tenant_id = o.tenant_id AND pp.order_id = o.order_id
                """;
    }

    private void appendBillAccessFilter(StringBuilder sql, List<Object> args, Long tenantId, Long memberId, Long houseId) {
        sql.append("""
                WHERE fb.tenant_id = ? AND fb.deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind mhb
                    WHERE mhb.tenant_id = fb.tenant_id AND mhb.project_id = fb.project_id
                      AND mhb.house_id = ? AND mhb.member_id = ? AND mhb.status = 'APPROVED'
                      AND (mhb.bind_role = 'OWNER' OR mhb.allow_bill = 1)
                      AND mhb.deleted = 0
                      AND (
                        fb.house_id = mhb.house_id
                        OR (fb.object_type = 'HOUSE' AND fb.object_id = mhb.house_id)
                        OR EXISTS (
                          SELECT 1 FROM base_parking_space s
                          WHERE s.tenant_id = fb.tenant_id AND s.project_id = fb.project_id
                            AND s.space_id = fb.object_id AND fb.object_type = 'SPACE'
                            AND s.house_id = mhb.house_id AND s.deleted = 0
                        )
                        OR EXISTS (
                          SELECT 1 FROM base_vehicle v
                          WHERE v.tenant_id = fb.tenant_id AND v.project_id = fb.project_id
                            AND v.vehicle_id = fb.object_id AND fb.object_type = 'VEHICLE'
                            AND v.member_id = mhb.member_id AND v.deleted = 0
                        )
                        OR fb.member_id = mhb.member_id
                      )
                  )
                """);
        args.add(tenantId);
        args.add(houseId);
        args.add(memberId);
    }

    private void appendMemberBillAccessFilter(StringBuilder sql, List<Object> args, Long memberId, String permissionColumn) {
        sql.append("""
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind mhb
                    WHERE mhb.tenant_id = fb.tenant_id AND mhb.project_id = fb.project_id
                      AND mhb.member_id = ? AND mhb.status = 'APPROVED'
                """);
        sql.append("                      AND (mhb.bind_role = 'OWNER' OR mhb.")
                .append(permissionColumn)
                .append(" = 1)\n");
        sql.append("""
                      AND mhb.deleted = 0
                      AND (
                        fb.house_id = mhb.house_id
                        OR (fb.object_type = 'HOUSE' AND fb.object_id = mhb.house_id)
                        OR EXISTS (
                          SELECT 1 FROM base_parking_space s
                          WHERE s.tenant_id = fb.tenant_id AND s.project_id = fb.project_id
                            AND s.space_id = fb.object_id AND fb.object_type = 'SPACE'
                            AND s.house_id = mhb.house_id AND s.deleted = 0
                        )
                        OR EXISTS (
                          SELECT 1 FROM base_vehicle v
                          WHERE v.tenant_id = fb.tenant_id AND v.project_id = fb.project_id
                            AND v.vehicle_id = fb.object_id AND fb.object_type = 'VEHICLE'
                            AND v.member_id = mhb.member_id AND v.deleted = 0
                        )
                        OR fb.member_id = mhb.member_id
                      )
                  )
                """);
        args.add(memberId);
    }

    private void appendBillStatusFilter(StringBuilder sql, List<Object> args, String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        if ("UNPAID".equals(status)) {
            sql.append(" AND fb.status IN ('UNPAID', 'OVERDUE', 'PARTIAL_PAID')");
            return;
        }
        sql.append(" AND fb.status = ?");
        args.add(status);
    }

    private void appendPayOrderFilter(StringBuilder sql, List<Object> args, Long tenantId, Long memberId, String status) {
        sql.append("""
                WHERE o.tenant_id = ? AND o.member_id = ? AND o.deleted = 0
                """);
        args.add(tenantId);
        args.add(memberId);
        if (status != null && !status.isBlank()) {
            if ("SUCCESS".equals(status)) {
                sql.append(" AND o.status IN ('PAID', 'REFUNDING', 'PARTIAL_REFUNDED', 'REFUNDED')");
                return;
            }
            sql.append(" AND o.status = ?");
            args.add(status);
        }
    }
}
