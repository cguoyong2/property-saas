package com.yongquan.propertysaas.importing.domain;

import java.time.LocalDateTime;

public record ImportBatchView(
        Long batchId,
        Long tenantId,
        Long projectId,
        String importType,
        String batchNo,
        Long sourceFileId,
        Integer totalCount,
        Integer successCount,
        Integer failCount,
        String importStatus,
        Long errorReportFileId,
        Boolean canRollback,
        String rollbackStatus,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
