package com.yongquan.propertysaas.fee.domain;

public record BillGenerateResultView(
        Long batchId,
        String batchNo,
        int totalCount,
        int successCount,
        int failCount,
        String status
) {
}
