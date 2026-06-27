package com.yongquan.propertysaas.payment.domain;

public record ReconcileReviewStatsView(
        Long totalCount,
        Long pendingCount,
        Long approvedCount,
        Long rejectedCount,
        Long resolvedCount,
        Long stillAbnormalCount
) {
}
