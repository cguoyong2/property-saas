package com.yongquan.propertysaas.report.service;

import com.yongquan.propertysaas.report.domain.FeeReportView;
import com.yongquan.propertysaas.report.domain.LeaseReportView;
import com.yongquan.propertysaas.report.domain.PatrolReportView;
import com.yongquan.propertysaas.report.domain.PlatformReportView;
import com.yongquan.propertysaas.report.domain.ReportCenterView;
import com.yongquan.propertysaas.report.domain.WorkOrderReportView;
import com.yongquan.propertysaas.report.repository.ReportRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    public ReportCenterView center(Long projectId, LocalDate startDate, LocalDate endDate) {
        return new ReportCenterView(
                feeSummary(projectId, startDate, endDate),
                workOrderSummary(projectId, startDate, endDate),
                patrolSummary(projectId, startDate, endDate),
                leaseSummary(projectId, startDate, endDate)
        );
    }

    public FeeReportView feeSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        Map<String, Object> data = repository.feeSummary(tenantId, scope, projectId, startDate, endDate);
        BigDecimal transactionPaid = repository.paidTransactionAmount(tenantId, scope, projectId, startDate, endDate);
        BigDecimal refundAmount = repository.refundTransactionAmount(tenantId, scope, projectId, startDate, endDate);
        BigDecimal receivable = decimal(data, "receivable_amount");
        BigDecimal netPaid = transactionPaid.subtract(refundAmount).max(BigDecimal.ZERO);
        return new FeeReportView(receivable, transactionPaid, refundAmount, netPaid, decimal(data, "arrears_amount"),
                rate(netPaid, receivable), number(data, "bill_count"), number(data, "unpaid_bill_count"), number(data, "paid_bill_count"));
    }

    public WorkOrderReportView workOrderSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        Map<String, Object> data = repository.workOrderSummary(tenantId, scope, projectId, startDate, endDate);
        long total = number(data, "total_count");
        long completed = number(data, "completed_count");
        return new WorkOrderReportView(total, number(data, "pending_count"), number(data, "processing_count"),
                number(data, "overdue_count"), completed, rate(BigDecimal.valueOf(completed), BigDecimal.valueOf(total)),
                repository.workOrderAverageScore(tenantId, scope, projectId, startDate, endDate).setScale(2, RoundingMode.HALF_UP));
    }

    public PatrolReportView patrolSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Map<String, Object> data = repository.patrolSummary(tenantId(), projectScope(tenantId()), projectId, startDate, endDate);
        long total = number(data, "task_count");
        long completed = number(data, "completed_count");
        long missed = number(data, "missed_count");
        return new PatrolReportView(total, completed, missed, number(data, "exception_count"), number(data, "rectified_count"),
                rate(BigDecimal.valueOf(completed), BigDecimal.valueOf(total)), rate(BigDecimal.valueOf(missed), BigDecimal.valueOf(total)));
    }

    public LeaseReportView leaseSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        Map<String, Object> data = repository.leaseSummary(tenantId, scope, projectId, startDate, endDate);
        Map<String, Object> rent = repository.leaseRentSummary(tenantId, scope, projectId, startDate, endDate);
        BigDecimal rentable = decimal(data, "rentable_area");
        BigDecimal leased = decimal(data, "leased_area");
        long customerCount = number(data, "customer_count");
        long signedCustomerCount = number(data, "signed_customer_count");
        return new LeaseReportView(rentable, leased, rate(leased, rentable), number(data, "vacant_resource_count"),
                number(data, "active_contract_count"), number(data, "expiring_contract_count"),
                decimal(rent, "rent_receivable_amount"), decimal(rent, "rent_paid_amount"), customerCount, signedCustomerCount,
                rate(BigDecimal.valueOf(signedCustomerCount), BigDecimal.valueOf(customerCount)));
    }

    public PlatformReportView platformSummary() {
        Map<String, Object> data = repository.platformSummary();
        return new PlatformReportView(number(data, "tenant_count"), number(data, "active_tenant_count"),
                number(data, "expiring_tenant_count"), number(data, "project_count"), number(data, "house_count"),
                number(data, "user_count"), number(data, "member_count"), number(data, "interface_failure_count"));
    }

    private void ensureProjectAllowed(Long projectId) {
        if (!repository.projectExists(tenantId(), projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private long number(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }
}
