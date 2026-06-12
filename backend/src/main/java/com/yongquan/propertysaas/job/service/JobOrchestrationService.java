package com.yongquan.propertysaas.job.service;

import com.yongquan.propertysaas.job.config.JobProperties;
import com.yongquan.propertysaas.job.domain.JobRunResult;
import com.yongquan.propertysaas.job.domain.JobRunSummary;
import com.yongquan.propertysaas.job.repository.JobRepository;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobOrchestrationService {

    public static final String JOB_WORK_ORDER_SLA = "WORK_ORDER_SLA_OVERDUE";
    public static final String JOB_PATROL_MISSED = "PATROL_MISSED";
    public static final String JOB_LEASE_EXPIRE_REMIND = "LEASE_EXPIRE_REMIND";
    public static final String JOB_MESSAGE_DISPATCH = "MESSAGE_DISPATCH";

    private final JobRepository repository;
    private final JobProperties properties;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public JobOrchestrationService(JobRepository repository, JobProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public JobRunResult runWorkOrderSla(Long tenantId, Integer limit) {
        LocalDateTime startedAt = LocalDateTime.now();
        int affected = 0;
        int effectiveLimit = effectiveLimit(limit);
        for (JobRepository.WorkOrderSlaCandidate candidate : repository.findSlaOverdueCandidates(tenantId, effectiveLimit)) {
            String content = "工单 " + candidate.orderNo() + " 已超过 SLA 截止时间 " + candidate.slaDeadline();
            repository.insertWorkOrderEvent(newId(), tenantId, candidate.projectId(), candidate.workOrderId(),
                    candidate.status(), "SLA_OVERDUE", content);
            for (NoticeRecipient recipient : repository.findProjectUserRecipients(tenantId, candidate.projectId())) {
                repository.insertMessage(newId(), tenantId, candidate.projectId(), recipient,
                        "WORKORDER_SLA_OVERDUE", "工单 SLA 超时", content);
            }
            affected++;
        }
        return success(JOB_WORK_ORDER_SLA, affected, "已标记 SLA 超时工单", startedAt);
    }

    @Transactional
    public JobRunResult runPatrolMissed(Long tenantId, Integer limit) {
        LocalDateTime startedAt = LocalDateTime.now();
        int affected = 0;
        int effectiveLimit = effectiveLimit(limit);
        for (JobRepository.PatrolMissedCandidate candidate : repository.findPatrolMissedCandidates(tenantId, effectiveLimit)) {
            repository.markPatrolTaskItemsMissed(tenantId, candidate.taskId());
            affected += repository.markPatrolTaskMissed(tenantId, candidate.taskId());
        }
        return success(JOB_PATROL_MISSED, affected, "已标记漏检巡检任务", startedAt);
    }

    @Transactional
    public JobRunResult runLeaseExpireRemind(Long tenantId, Integer days, Integer limit) {
        LocalDateTime startedAt = LocalDateTime.now();
        int affected = 0;
        int effectiveDays = days == null ? properties.getLeaseExpireDays() : Math.max(days, 0);
        int effectiveLimit = effectiveLimit(limit);
        for (JobRepository.LeaseExpireCandidate candidate : repository.findLeaseExpireCandidates(tenantId, effectiveDays, effectiveLimit)) {
            String content = "合同 " + candidate.contractNo() + " 将于 " + candidate.endDate() + " 到期";
            for (NoticeRecipient recipient : repository.findProjectUserRecipients(tenantId, candidate.projectId())) {
                repository.insertMessage(newId(), tenantId, candidate.projectId(), recipient,
                        "LEASE_CONTRACT_EXPIRE", "租赁合同到期提醒", content);
            }
            affected++;
        }
        return success(JOB_LEASE_EXPIRE_REMIND, affected, "已生成租赁合同到期站内提醒", startedAt);
    }

    @Transactional
    public JobRunResult runMessageDispatch(Long tenantId, Integer limit) {
        LocalDateTime startedAt = LocalDateTime.now();
        int affected = repository.dispatchPendingSiteMessages(tenantId, effectiveLimit(limit));
        return success(JOB_MESSAGE_DISPATCH, affected, "已派发待发送站内信", startedAt);
    }

    public JobRunSummary runAllForCurrentScope(Integer limit, Integer leaseExpireDays) {
        if (TenantContext.hasTenant()) {
            return runAllForTenant(TenantContext.requiredTenantId(), limit, leaseExpireDays);
        }
        return runAllTenants(limit, leaseExpireDays);
    }

    public JobRunSummary runAllForTenant(Long tenantId, Integer limit, Integer leaseExpireDays) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<JobRunResult> results = List.of(
                runWorkOrderSla(tenantId, limit),
                runPatrolMissed(tenantId, limit),
                runLeaseExpireRemind(tenantId, leaseExpireDays, limit),
                runMessageDispatch(tenantId, limit)
        );
        return new JobRunSummary("TENANT:" + tenantId, 1, totalAffected(results), startedAt, LocalDateTime.now(), results);
    }

    public JobRunSummary runWorkOrderSlaForAllTenants(Integer limit) {
        return runSingleJobForAllTenants(JOB_WORK_ORDER_SLA, tenantId -> runWorkOrderSla(tenantId, limit));
    }

    public JobRunSummary runPatrolMissedForAllTenants(Integer limit) {
        return runSingleJobForAllTenants(JOB_PATROL_MISSED, tenantId -> runPatrolMissed(tenantId, limit));
    }

    public JobRunSummary runLeaseExpireRemindForAllTenants(Integer days, Integer limit) {
        return runSingleJobForAllTenants(JOB_LEASE_EXPIRE_REMIND,
                tenantId -> runLeaseExpireRemind(tenantId, days, limit));
    }

    public JobRunSummary runMessageDispatchForAllTenants(Integer limit) {
        return runSingleJobForAllTenants(JOB_MESSAGE_DISPATCH, tenantId -> runMessageDispatch(tenantId, limit));
    }

    public JobRunSummary runAllTenants(Integer limit, Integer leaseExpireDays) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<Long> tenantIds = repository.findRunnableTenantIds();
        List<JobRunResult> results = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            results.add(runWorkOrderSla(tenantId, limit));
            results.add(runPatrolMissed(tenantId, limit));
            results.add(runLeaseExpireRemind(tenantId, leaseExpireDays, limit));
            results.add(runMessageDispatch(tenantId, limit));
        }
        return new JobRunSummary("ALL_TENANTS", tenantIds.size(), totalAffected(results), startedAt, LocalDateTime.now(), results);
    }

    private JobRunSummary runSingleJobForAllTenants(String jobCode, TenantJobRunner runner) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<Long> tenantIds = repository.findRunnableTenantIds();
        List<JobRunResult> results = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            results.add(runner.run(tenantId));
        }
        return new JobRunSummary(jobCode + ":ALL_TENANTS", tenantIds.size(), totalAffected(results),
                startedAt, LocalDateTime.now(), results);
    }

    private JobRunResult success(String jobCode, int affected, String message, LocalDateTime startedAt) {
        return new JobRunResult(jobCode, true, affected, message, startedAt, LocalDateTime.now());
    }

    private int totalAffected(List<JobRunResult> results) {
        return results.stream().mapToInt(JobRunResult::affectedCount).sum();
    }

    private int effectiveLimit(Integer limit) {
        int value = limit == null ? properties.getDefaultLimit() : limit;
        return Math.max(1, Math.min(value, 1000));
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }

    private interface TenantJobRunner {
        JobRunResult run(Long tenantId);
    }
}
