package com.yongquan.propertysaas.report.domain;

import java.math.BigDecimal;

public record PatrolReportView(
        long taskCount,
        long completedCount,
        long missedCount,
        long exceptionCount,
        long rectifiedCount,
        BigDecimal completionRate,
        BigDecimal missedRate
) {
}
