package com.yongquan.propertysaas.report.domain;

public record ReportCenterView(
        FeeReportView fee,
        WorkOrderReportView workOrder,
        PatrolReportView patrol,
        LeaseReportView lease
) {
}
