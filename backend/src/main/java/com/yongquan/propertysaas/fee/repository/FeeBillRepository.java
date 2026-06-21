package com.yongquan.propertysaas.fee.repository;

import com.yongquan.propertysaas.fee.domain.BillObjectTarget;
import com.yongquan.propertysaas.fee.domain.BillStandardCandidate;
import com.yongquan.propertysaas.fee.domain.FeeBillView;
import com.yongquan.propertysaas.fee.dto.BillManualRequest;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class FeeBillRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public FeeBillRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<FeeBillView> findBills(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status,
                                       String billPeriod, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT bill_id, project_id, bill_no, item_id, standard_id, object_type, object_id, member_id,
                       house_id, bill_period, receivable_amount, discount_amount, paid_amount, refund_amount,
                       remaining_amount, due_date, status, source_type, void_reason, created_at
                FROM fee_bill
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendBillFilters(sql, args, projectId, status, billPeriod);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, bill_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBill, args.toArray());
    }

    public long countBills(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status, String billPeriod) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fee_bill WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendBillFilters(sql, args, projectId, status, billPeriod);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public FeeBillView getBill(Long tenantId, Long billId) {
        return jdbcTemplate.queryForObject("""
                SELECT bill_id, project_id, bill_no, item_id, standard_id, object_type, object_id, member_id,
                       house_id, bill_period, receivable_amount, discount_amount, paid_amount, refund_amount,
                       remaining_amount, due_date, status, source_type, void_reason, created_at
                FROM fee_bill
                WHERE tenant_id = ? AND bill_id = ? AND deleted = 0
                """, this::mapBill, tenantId, billId);
    }

    public List<BillStandardCandidate> findGenerateCandidates(Long tenantId, Long projectId, Long itemId,
                                                              String objectType, List<Long> objectIds,
                                                              LocalDate billDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT b.project_id, s.item_id, b.standard_id, s.charge_method, s.unit_price, s.cycle, s.formula,
                       b.object_type, b.object_id
                FROM fee_standard_bind b
                JOIN fee_standard s ON s.tenant_id = b.tenant_id AND s.standard_id = b.standard_id
                WHERE b.tenant_id = ? AND b.project_id = ? AND s.item_id = ?
                  AND b.deleted = 0 AND s.deleted = 0
                  AND b.status = 'ACTIVE' AND s.status = 'ACTIVE'
                  AND b.effective_date <= ? AND (b.expire_date IS NULL OR b.expire_date >= ?)
                  AND s.effective_date <= ? AND (s.expire_date IS NULL OR s.expire_date >= ?)
                """);
        args.add(tenantId);
        args.add(projectId);
        args.add(itemId);
        args.add(billDate);
        args.add(billDate);
        args.add(billDate);
        args.add(billDate);
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND b.object_type = ?");
            args.add(objectType);
        }
        if (objectIds != null && !objectIds.isEmpty()) {
            sql.append(" AND b.object_id IN (");
            sql.append("?,".repeat(objectIds.size()));
            sql.setLength(sql.length() - 1);
            sql.append(")");
            args.addAll(objectIds);
        }
        sql.append(" ORDER BY b.object_type ASC, b.object_id ASC, b.bind_id ASC");
        return jdbcTemplate.query(sql.toString(), this::mapCandidate, args.toArray());
    }

    public void insertBill(Long tenantId, Long billId, String billNo, Long userId, BillManualRequest request,
                           BigDecimal receivableAmount, BigDecimal discountAmount, BigDecimal remainingAmount,
                           String sourceType) {
        jdbcTemplate.update("""
                        INSERT INTO fee_bill(bill_id, tenant_id, project_id, bill_no, item_id, standard_id,
                                             object_type, object_id, member_id, house_id, bill_period,
                                             receivable_amount, discount_amount, paid_amount, refund_amount,
                                             remaining_amount, due_date, status, source_type, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.00, 0.00, ?, ?, 'UNPAID', ?, ?)
                        """,
                billId, tenantId, request.projectId(), billNo, request.itemId(), request.standardId(),
                request.objectType(), request.objectId(), request.memberId(), request.houseId(), request.billPeriod(),
                receivableAmount, discountAmount, remainingAmount, request.dueDate(), sourceType, userId);
    }

    public int voidBill(Long tenantId, Long billId, Long userId, String reason) {
        return jdbcTemplate.update("""
                        UPDATE fee_bill
                        SET status = 'VOID', void_reason = ?, updated_by = ?
                        WHERE tenant_id = ? AND bill_id = ? AND deleted = 0
                          AND status IN ('UNPAID', 'OVERDUE', 'PARTIAL_PAID')
                        """, reason, userId, tenantId, billId);
    }

    public boolean duplicateBillExists(Long tenantId, Long projectId, String objectType, Long objectId,
                                       Long itemId, String billPeriod) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM fee_bill
                WHERE tenant_id = ? AND project_id = ? AND object_type = ? AND object_id = ?
                  AND item_id = ? AND bill_period = ? AND deleted = 0 AND status <> 'VOID'
                """, Integer.class, tenantId, projectId, objectType, objectId, itemId, billPeriod);
        return count != null && count > 0;
    }

    public boolean itemExists(Long tenantId, Long itemId) {
        return exists("SELECT COUNT(*) FROM fee_item WHERE tenant_id = ? AND item_id = ? AND deleted = 0", tenantId, itemId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public boolean standardExists(Long tenantId, Long itemId, Long standardId, Long projectId) {
        if (standardId == null) {
            return true;
        }
        return exists("""
                SELECT COUNT(*) FROM fee_standard
                WHERE tenant_id = ? AND item_id = ? AND standard_id = ? AND deleted = 0
                  AND (project_id IS NULL OR project_id = ?)
                """, tenantId, itemId, standardId, projectId);
    }

    public boolean objectExists(Long tenantId, Long projectId, String objectType, Long objectId) {
        return switch (objectType) {
            case "HOUSE" -> exists("""
                    SELECT COUNT(*) FROM base_house
                    WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0
                    """, tenantId, projectId, objectId);
            case "VEHICLE" -> exists("""
                    SELECT COUNT(*) FROM base_vehicle
                    WHERE tenant_id = ? AND project_id = ? AND vehicle_id = ? AND deleted = 0
                    """, tenantId, projectId, objectId);
            case "SPACE" -> exists("""
                    SELECT COUNT(*) FROM base_parking_space
                    WHERE tenant_id = ? AND project_id = ? AND space_id = ? AND deleted = 0
                    """, tenantId, projectId, objectId);
            default -> false;
        };
    }

    public BillObjectTarget resolveObjectTarget(Long tenantId, Long projectId, String objectType, Long objectId) {
        try {
            return switch (objectType) {
                case "HOUSE" -> jdbcTemplate.queryForObject("""
                        SELECT member_id, house_id, mobile
                        FROM member_house_bind
                        WHERE tenant_id = ? AND project_id = ? AND house_id = ?
                          AND status = 'APPROVED' AND deleted = 0
                        ORDER BY bind_role = 'OWNER' DESC, created_at DESC
                        LIMIT 1
                        """, this::mapTarget, tenantId, projectId, objectId);
                case "VEHICLE" -> jdbcTemplate.queryForObject("""
                        SELECT v.member_id, v.house_id, m.mobile
                        FROM base_vehicle v
                        LEFT JOIN member_user m ON m.tenant_id = v.tenant_id AND m.member_id = v.member_id AND m.deleted = 0
                        WHERE v.tenant_id = ? AND v.project_id = ? AND v.vehicle_id = ? AND v.deleted = 0
                        """, this::mapTarget, tenantId, projectId, objectId);
                case "SPACE" -> jdbcTemplate.queryForObject("""
                        SELECT b.member_id, s.house_id, b.mobile
                        FROM base_parking_space s
                        LEFT JOIN member_house_bind b ON b.tenant_id = s.tenant_id AND b.project_id = s.project_id
                             AND b.house_id = s.house_id AND b.status = 'APPROVED' AND b.deleted = 0
                        WHERE s.tenant_id = ? AND s.project_id = ? AND s.space_id = ? AND s.deleted = 0
                        ORDER BY b.bind_role = 'OWNER' DESC, b.created_at DESC
                        LIMIT 1
                        """, this::mapTarget, tenantId, projectId, objectId);
                default -> new BillObjectTarget(null, null, null);
            };
        } catch (EmptyResultDataAccessException ex) {
            return new BillObjectTarget(null, "HOUSE".equals(objectType) ? objectId : null, null);
        }
    }

    public BigDecimal findHouseBuildingArea(Long tenantId, Long projectId, Long houseId) {
        return jdbcTemplate.queryForObject("""
                SELECT building_area
                FROM base_house
                WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0
                """, BigDecimal.class, tenantId, projectId, houseId);
    }

    public void insertImportBatch(Long tenantId, Long projectId, Long batchId, String batchNo, Long sourceFileId,
                                  int totalCount, int successCount, int failCount, String status, Long userId,
                                  String importType) {
        jdbcTemplate.update("""
                        INSERT INTO import_batch(batch_id, tenant_id, project_id, import_type, batch_no, source_file_id,
                                                 total_count, success_count, fail_count, import_status, can_rollback,
                                                 rollback_status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'NONE', ?)
                        """, batchId, tenantId, projectId, importType, batchNo, sourceFileId, totalCount,
                successCount, failCount, status, userId);
    }

    public void insertImportError(Long tenantId, Long projectId, Long batchId, Long errorId, int rowNo,
                                  String fieldName, String rawValue, String errorCode, String errorMessage) {
        jdbcTemplate.update("""
                        INSERT INTO import_error_detail(error_id, tenant_id, project_id, batch_id, row_no,
                                                        field_name, raw_value, error_code, error_message)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, errorId, tenantId, projectId, batchId, rowNo, fieldName, rawValue, errorCode, errorMessage);
    }

    public void insertMessage(Long tenantId, Long projectId, Long messageId, String receiverType, Long receiverId,
                              String receiverMobile, String channel, String templateCode, String title, String content) {
        jdbcTemplate.update("""
                        INSERT INTO message_record(message_id, tenant_id, project_id, receiver_type, receiver_id,
                                                   receiver_mobile, channel, template_code, title, content, send_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, messageId, tenantId, projectId, receiverType, receiverId, receiverMobile, channel,
                templateCode, title, content);
    }

    private void appendBillFilters(StringBuilder sql, List<Object> args, Long projectId, String status, String billPeriod) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (billPeriod != null && !billPeriod.isBlank()) {
            sql.append(" AND bill_period = ?");
            args.add(billPeriod);
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

    private FeeBillView mapBill(ResultSet rs, int rowNum) throws SQLException {
        return new FeeBillView(rs.getLong("bill_id"), rs.getLong("project_id"), rs.getString("bill_no"),
                rs.getLong("item_id"), (Long) rs.getObject("standard_id"), rs.getString("object_type"),
                rs.getLong("object_id"), (Long) rs.getObject("member_id"), (Long) rs.getObject("house_id"),
                rs.getString("bill_period"), rs.getBigDecimal("receivable_amount"),
                rs.getBigDecimal("discount_amount"), rs.getBigDecimal("paid_amount"), rs.getBigDecimal("refund_amount"),
                rs.getBigDecimal("remaining_amount"), rs.getObject("due_date", LocalDate.class),
                rs.getString("status"), rs.getString("source_type"), rs.getString("void_reason"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private BillStandardCandidate mapCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new BillStandardCandidate(rs.getLong("project_id"), rs.getLong("item_id"), rs.getLong("standard_id"),
                rs.getString("charge_method"), rs.getBigDecimal("unit_price"), rs.getString("cycle"),
                rs.getString("formula"), rs.getString("object_type"), rs.getLong("object_id"));
    }

    private BillObjectTarget mapTarget(ResultSet rs, int rowNum) throws SQLException {
        return new BillObjectTarget((Long) rs.getObject("member_id"), (Long) rs.getObject("house_id"),
                rs.getString("mobile"));
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
