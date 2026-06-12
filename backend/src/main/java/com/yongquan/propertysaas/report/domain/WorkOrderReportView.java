package com.yongquan.propertysaas.report.domain;

import java.math.BigDecimal;

public record WorkOrderReportView(
        long totalCount,
        long pendingCount,
        long processingCount,
        long overdueCount,
        long completedCount,
        BigDecimal completionRate,
        BigDecimal averageScore
) {
}
