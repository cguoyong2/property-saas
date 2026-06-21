package com.yongquan.propertysaas.job.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.job.domain.JobRunResult;
import com.yongquan.propertysaas.job.domain.JobRunSummary;
import com.yongquan.propertysaas.job.service.JobOrchestrationService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final JobOrchestrationService service;

    public JobController(JobOrchestrationService service) {
        this.service = service;
    }

    @PostMapping("/api/jobs/run-all")
    @RequiresPermission("job:run")
    public ApiResponse<JobRunSummary> runAll(@RequestParam(required = false) Integer limit,
                                             @RequestParam(required = false) Integer leaseExpireDays) {
        return ApiResponse.success(service.runAllForCurrentScope(limit, leaseExpireDays));
    }

    @PostMapping("/api/jobs/workorder-sla")
    @RequiresPermission("job:workorder:sla")
    public ApiResponse<JobRunResult> workOrderSla(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.runWorkOrderSla(TenantContext.requiredTenantId(), limit));
    }

    @PostMapping("/api/jobs/patrol-missed")
    @RequiresPermission("job:patrol:missed")
    public ApiResponse<JobRunResult> patrolMissed(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.runPatrolMissed(TenantContext.requiredTenantId(), limit));
    }

    @PostMapping("/api/jobs/lease-expire-remind")
    @RequiresPermission("job:lease:remind")
    public ApiResponse<JobRunResult> leaseExpireRemind(@RequestParam(required = false) Integer days,
                                                       @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.runLeaseExpireRemind(TenantContext.requiredTenantId(), days, limit));
    }

    @PostMapping("/api/jobs/message-dispatch")
    @RequiresPermission("job:message:dispatch")
    public ApiResponse<JobRunResult> messageDispatch(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.runMessageDispatch(TenantContext.requiredTenantId(), limit));
    }

    @PostMapping("/api/jobs/fee-bill-generate")
    @RequiresPermission("job:fee:bill:generate")
    public ApiResponse<JobRunResult> feeBillGenerate(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.runFeeBillGenerate(TenantContext.requiredTenantId(), limit));
    }
}
