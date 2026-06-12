package com.yongquan.propertysaas.system.audit.domain;

import java.time.LocalDateTime;

public record OperationLogView(
        Long logId,
        Long tenantId,
        Long projectId,
        String operatorType,
        Long operatorId,
        String moduleCode,
        String actionCode,
        String objectType,
        Long objectId,
        String beforeData,
        String afterData,
        String reason,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
