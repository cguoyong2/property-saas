package com.yongquan.propertysaas.payment.domain;

public record ReconcileExceptionStatsView(
        Long totalCount,
        Long highRiskCount,
        Long mediumRiskCount,
        Long lowRiskCount,
        Long openCount,
        Long pendingReviewCount,
        Long handledCount
) {
}
