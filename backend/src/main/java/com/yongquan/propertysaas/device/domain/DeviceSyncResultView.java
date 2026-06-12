package com.yongquan.propertysaas.device.domain;

public record DeviceSyncResultView(
        int totalCount,
        int successCount,
        int failureCount
) {
}
