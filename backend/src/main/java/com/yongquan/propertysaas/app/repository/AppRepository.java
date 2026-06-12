package com.yongquan.propertysaas.app.repository;

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

    public List<Map<String, Object>> findBills(Long tenantId, Long memberId, Long houseId, long offset, long pageSize) {
        return jdbcTemplate.queryForList("""
                SELECT bill_id AS billId, project_id AS projectId, bill_no AS billNo, item_id AS itemId,
                       object_type AS objectType, object_id AS objectId, member_id AS memberId, house_id AS houseId,
                       bill_period AS billPeriod, receivable_amount AS receivableAmount,
                       discount_amount AS discountAmount, paid_amount AS paidAmount, refund_amount AS refundAmount,
                       remaining_amount AS remainingAmount, due_date AS dueDate, status, source_type AS sourceType,
                       created_at AS createdAt
                FROM fee_bill
                WHERE tenant_id = ? AND house_id = ? AND deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind b
                    WHERE b.tenant_id = fee_bill.tenant_id AND b.project_id = fee_bill.project_id
                      AND b.house_id = fee_bill.house_id AND b.member_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                  )
                ORDER BY due_date IS NULL, due_date ASC, created_at DESC
                LIMIT ? OFFSET ?
                """, tenantId, houseId, memberId, pageSize, offset);
    }

    public long countBills(Long tenantId, Long memberId, Long houseId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM fee_bill
                WHERE tenant_id = ? AND house_id = ? AND deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind b
                    WHERE b.tenant_id = fee_bill.tenant_id AND b.project_id = fee_bill.project_id
                      AND b.house_id = fee_bill.house_id AND b.member_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                  )
                """, Long.class, tenantId, houseId, memberId);
        return count == null ? 0 : count;
    }

    public Map<String, Object> getBill(Long tenantId, Long memberId, Long billId) {
        return jdbcTemplate.queryForMap("""
                SELECT bill_id AS billId, project_id AS projectId, bill_no AS billNo, item_id AS itemId,
                       object_type AS objectType, object_id AS objectId, member_id AS memberId, house_id AS houseId,
                       bill_period AS billPeriod, receivable_amount AS receivableAmount,
                       discount_amount AS discountAmount, paid_amount AS paidAmount, refund_amount AS refundAmount,
                       remaining_amount AS remainingAmount, due_date AS dueDate, status, source_type AS sourceType,
                       created_at AS createdAt
                FROM fee_bill
                WHERE tenant_id = ? AND bill_id = ? AND deleted = 0
                  AND EXISTS (
                    SELECT 1 FROM member_house_bind b
                    WHERE b.tenant_id = fee_bill.tenant_id AND b.project_id = fee_bill.project_id
                      AND b.house_id = fee_bill.house_id AND b.member_id = ? AND b.status = 'APPROVED' AND b.deleted = 0
                  )
                """, tenantId, billId, memberId);
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
}
