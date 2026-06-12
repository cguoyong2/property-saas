package com.yongquan.propertysaas.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public ReportRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public Map<String, Object> feeSummary(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(receivable_amount), 0) AS receivable_amount,
                       COALESCE(SUM(paid_amount), 0) AS paid_amount,
                       COALESCE(SUM(refund_amount), 0) AS refund_amount,
                       COALESCE(SUM(remaining_amount), 0) AS arrears_amount,
                       COUNT(*) AS bill_count,
                       SUM(CASE WHEN status IN ('UNPAID', 'PARTIAL_PAID', 'OVERDUE') THEN 1 ELSE 0 END) AS unpaid_bill_count,
                       SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_bill_count
                FROM fee_bill
                WHERE tenant_id = ? AND deleted = 0 AND status <> 'VOID'
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendPeriodRange(sql, args, startDate, endDate, "bill_period");
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public BigDecimal paidTransactionAmount(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(amount), 0)
                FROM pay_transaction
                WHERE tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendDateTimeRange(sql, args, startDate, endDate, "paid_at");
        return decimal(jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, args.toArray()));
    }

    public BigDecimal refundTransactionAmount(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(refund_amount), 0)
                FROM pay_refund_transaction
                WHERE tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendDateTimeRange(sql, args, startDate, endDate, "refunded_at");
        return decimal(jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, args.toArray()));
    }

    public Map<String, Object> workOrderSummary(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS total_count,
                       SUM(CASE WHEN status = 'SUBMITTED' THEN 1 ELSE 0 END) AS pending_count,
                       SUM(CASE WHEN status IN ('ACCEPTED', 'DISPATCHED', 'PROCESSING', 'HANG_UP', 'WAIT_CONFIRM', 'REWORK') THEN 1 ELSE 0 END) AS processing_count,
                       SUM(CASE WHEN sla_deadline IS NOT NULL AND sla_deadline < NOW()
                                 AND status NOT IN ('COMPLETED', 'EVALUATED', 'CANCELLED', 'REJECTED') THEN 1 ELSE 0 END) AS overdue_count,
                       SUM(CASE WHEN status IN ('COMPLETED', 'EVALUATED') THEN 1 ELSE 0 END) AS completed_count
                FROM work_order
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendDateTimeRange(sql, args, startDate, endDate, "created_at");
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public BigDecimal workOrderAverageScore(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(AVG(c.score), 0)
                FROM work_order_comment c
                JOIN work_order w ON w.tenant_id = c.tenant_id AND w.work_order_id = c.work_order_id AND w.deleted = 0
                WHERE c.tenant_id = ?
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId, "c.project_id");
        appendProjectScope(sql, args, scope, "c.project_id");
        appendDateTimeRange(sql, args, startDate, endDate, "c.created_at");
        return decimal(jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, args.toArray()));
    }

    public Map<String, Object> patrolSummary(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS task_count,
                       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
                       SUM(CASE WHEN status = 'MISSED' THEN 1 ELSE 0 END) AS missed_count,
                       SUM(CASE WHEN status IN ('EXCEPTION', 'RECTIFYING', 'RECTIFIED') THEN 1 ELSE 0 END) AS exception_count,
                       SUM(CASE WHEN status = 'RECTIFIED' THEN 1 ELSE 0 END) AS rectified_count
                FROM patrol_task
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendDateTimeRange(sql, args, startDate, endDate, "planned_start_at");
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public Map<String, Object> leaseSummary(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(area), 0) AS rentable_area,
                       COALESCE(SUM(CASE WHEN status = 'LEASED' THEN area ELSE 0 END), 0) AS leased_area,
                       SUM(CASE WHEN status = 'VACANT' THEN 1 ELSE 0 END) AS vacant_resource_count
                FROM lease_resource
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        Map<String, Object> resource = jdbcTemplate.queryForMap(sql.toString(), args.toArray());

        List<Object> contractArgs = new ArrayList<>();
        StringBuilder contractSql = new StringBuilder("""
                SELECT COUNT(*) AS active_contract_count,
                       SUM(CASE WHEN status = 'ACTIVE' AND end_date >= CURDATE()
                                 AND end_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS expiring_contract_count
                FROM lease_contract
                WHERE tenant_id = ? AND deleted = 0
                """);
        contractArgs.add(tenantId);
        appendProjectEquals(contractSql, contractArgs, projectId);
        appendProjectScope(contractSql, contractArgs, scope);
        appendDateRange(contractSql, contractArgs, startDate, endDate, "start_date");
        Map<String, Object> contract = jdbcTemplate.queryForMap(contractSql.toString(), contractArgs.toArray());

        List<Object> customerArgs = new ArrayList<>();
        StringBuilder customerSql = new StringBuilder("""
                SELECT COUNT(*) AS customer_count,
                       SUM(CASE WHEN status = 'SIGNED' THEN 1 ELSE 0 END) AS signed_customer_count
                FROM lease_customer
                WHERE tenant_id = ? AND deleted = 0
                """);
        customerArgs.add(tenantId);
        appendProjectEquals(customerSql, customerArgs, projectId);
        appendProjectScope(customerSql, customerArgs, scope);
        appendDateTimeRange(customerSql, customerArgs, startDate, endDate, "created_at");
        Map<String, Object> customer = jdbcTemplate.queryForMap(customerSql.toString(), customerArgs.toArray());

        resource.putAll(contract);
        resource.putAll(customer);
        return resource;
    }

    public Map<String, Object> leaseRentSummary(Long tenantId, List<Long> scope, Long projectId, LocalDate startDate, LocalDate endDate) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(receivable_amount), 0) AS rent_receivable_amount,
                       COALESCE(SUM(paid_amount), 0) AS rent_paid_amount
                FROM fee_bill
                WHERE tenant_id = ? AND deleted = 0 AND object_type = 'CONTRACT'
                  AND source_type = 'LEASE_CONTRACT' AND status <> 'VOID'
                """);
        args.add(tenantId);
        appendProjectEquals(sql, args, projectId);
        appendProjectScope(sql, args, scope);
        appendPeriodRange(sql, args, startDate, endDate, "bill_period");
        return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
    }

    public Map<String, Object> platformSummary() {
        return jdbcTemplate.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0) AS tenant_count,
                  (SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0 AND status = 'ACTIVE') AS active_tenant_count,
                  (SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0 AND service_end_date IS NOT NULL
                     AND service_end_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)) AS expiring_tenant_count,
                  (SELECT COUNT(*) FROM base_project WHERE deleted = 0) AS project_count,
                  (SELECT COUNT(*) FROM base_house WHERE deleted = 0) AS house_count,
                  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0) AS user_count,
                  (SELECT COUNT(*) FROM member_user WHERE deleted = 0) AS member_count,
                  (SELECT COUNT(*) FROM interface_call_log WHERE success = 0) AS interface_failure_count
                """);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    private void appendProjectEquals(StringBuilder sql, List<Object> args, Long projectId) {
        appendProjectEquals(sql, args, projectId, "project_id");
    }

    private void appendProjectEquals(StringBuilder sql, List<Object> args, Long projectId, String field) {
        if (projectId != null) {
            sql.append(" AND ").append(field).append(" = ?");
            args.add(projectId);
        }
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds, String field) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND ").append(field).append(" IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private void appendPeriodRange(StringBuilder sql, List<Object> args, LocalDate startDate, LocalDate endDate, String field) {
        if (startDate != null) {
            sql.append(" AND ").append(field).append(" >= ?");
            args.add(startDate.toString().substring(0, 7));
        }
        if (endDate != null) {
            sql.append(" AND ").append(field).append(" <= ?");
            args.add(endDate.toString().substring(0, 7));
        }
    }

    private void appendDateRange(StringBuilder sql, List<Object> args, LocalDate startDate, LocalDate endDate, String field) {
        if (startDate != null) {
            sql.append(" AND ").append(field).append(" >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND ").append(field).append(" <= ?");
            args.add(endDate);
        }
    }

    private void appendDateTimeRange(StringBuilder sql, List<Object> args, LocalDate startDate, LocalDate endDate, String field) {
        if (startDate != null) {
            sql.append(" AND ").append(field).append(" >= ?");
            args.add(startDate.atStartOfDay());
        }
        if (endDate != null) {
            sql.append(" AND ").append(field).append(" < ?");
            args.add(endDate.plusDays(1).atStartOfDay());
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private BigDecimal decimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
