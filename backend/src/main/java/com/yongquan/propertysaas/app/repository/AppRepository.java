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
                       h.unit_id AS unitId, h.building_area AS buildingArea, h.house_status AS houseStatus,
                       b.bind_role AS bindRole, b.status, b.created_at AS createdAt
                FROM member_house_bind b
                JOIN base_project p ON p.tenant_id = b.tenant_id AND p.project_id = b.project_id AND p.deleted = 0
                JOIN base_house h ON h.tenant_id = b.tenant_id AND h.project_id = b.project_id
                                 AND h.house_id = b.house_id AND h.deleted = 0
                WHERE b.tenant_id = ? AND b.member_id = ? AND b.deleted = 0
                ORDER BY b.status = 'APPROVED' DESC, b.created_at DESC
                """, tenantId, memberId);
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
        String sql = billSelectSql() + """
                WHERE fb.tenant_id = ? AND fb.bill_id = ? AND fb.deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind mhb
                    WHERE mhb.tenant_id = fb.tenant_id AND mhb.project_id = fb.project_id
                      AND mhb.house_id = fb.house_id AND mhb.member_id = ? AND mhb.status = 'APPROVED'
                      AND mhb.deleted = 0
                  )
                """;
        return jdbcTemplate.queryForMap(sql, tenantId, billId, memberId);
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
        sql.append("""
                )
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind mhb
                    WHERE mhb.tenant_id = fb.tenant_id AND mhb.project_id = fb.project_id
                      AND mhb.house_id = fb.house_id AND mhb.member_id = ? AND mhb.status = 'APPROVED'
                      AND mhb.deleted = 0
                  )
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(projectId);
        args.addAll(distinctBillIds);
        args.add(memberId);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
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
                    AND EXISTS (SELECT 1 FROM member_house_bind b WHERE b.tenant_id = fb.tenant_id AND b.project_id = fb.project_id
                      AND b.house_id = fb.house_id AND b.member_id = ? AND b.status = 'APPROVED' AND b.deleted = 0)) AS unpaidBillCount,
                  (SELECT COALESCE(SUM(fb.remaining_amount), 0) FROM fee_bill fb WHERE fb.tenant_id = ? AND fb.deleted = 0 AND fb.status IN ('UNPAID','OVERDUE','PARTIAL_PAID')
                    AND EXISTS (SELECT 1 FROM member_house_bind b WHERE b.tenant_id = fb.tenant_id AND b.project_id = fb.project_id
                      AND b.house_id = fb.house_id AND b.member_id = ? AND b.status = 'APPROVED' AND b.deleted = 0)) AS unpaidAmount,
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

    private void appendBillAccessFilter(StringBuilder sql, List<Object> args, Long tenantId, Long memberId, Long houseId) {
        sql.append("""
                WHERE fb.tenant_id = ? AND fb.house_id = ? AND fb.deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind mhb
                    WHERE mhb.tenant_id = fb.tenant_id AND mhb.project_id = fb.project_id
                      AND mhb.house_id = fb.house_id AND mhb.member_id = ? AND mhb.status = 'APPROVED'
                      AND mhb.deleted = 0
                  )
                """);
        args.add(tenantId);
        args.add(houseId);
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
}
