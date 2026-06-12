package com.yongquan.propertysaas.service.domain;

public record MessageDispatchResult(
        int totalCount,
        int sentCount,
        int failedCount,
        int skippedCount
) {
}
