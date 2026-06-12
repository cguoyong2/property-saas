package com.yongquan.propertysaas.report.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.report.domain.FeeReportView;
import com.yongquan.propertysaas.report.domain.LeaseReportView;
import com.yongquan.propertysaas.report.domain.PatrolReportView;
import com.yongquan.propertysaas.report.domain.PlatformReportView;
import com.yongquan.propertysaas.report.domain.ReportCenterView;
import com.yongquan.propertysaas.report.domain.WorkOrderReportView;
import com.yongquan.propertysaas.report.service.ReportService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/api/reports")
    @RequiresPermission("report:center:view")
    public ApiResponse<ReportCenterView> center(@RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.center(projectId, startDate, endDate));
    }

    @GetMapping("/api/report/fee/summary")
    @RequiresPermission("report:fee:view")
    public ApiResponse<FeeReportView> feeSummary(@RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.feeSummary(projectId, startDate, endDate));
    }

    @GetMapping("/api/report/workorders/summary")
    @RequiresPermission("report:workorder:view")
    public ApiResponse<WorkOrderReportView> workOrderSummary(@RequestParam(required = false) Long projectId,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.workOrderSummary(projectId, startDate, endDate));
    }

    @GetMapping("/api/report/patrol/summary")
    @RequiresPermission("report:patrol:view")
    public ApiResponse<PatrolReportView> patrolSummary(@RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.patrolSummary(projectId, startDate, endDate));
    }

    @GetMapping("/api/report/lease/summary")
    @RequiresPermission("report:lease:view")
    public ApiResponse<LeaseReportView> leaseSummary(@RequestParam(required = false) Long projectId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(service.leaseSummary(projectId, startDate, endDate));
    }

    @GetMapping("/api/report/platform/summary")
    @RequiresPermission(value = "report:platform:view", platformOnly = true)
    public ApiResponse<PlatformReportView> platformSummary() {
        return ApiResponse.success(service.platformSummary());
    }
}
