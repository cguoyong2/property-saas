package com.yongquan.propertysaas.base.domain;

public record ImportResultView(
        Long batchId,
        String batchNo,
        int totalCount,
        int successCount,
        int failCount,
        String importStatus
) {
}
