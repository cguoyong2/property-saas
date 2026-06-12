package com.yongquan.propertysaas.system.audit.domain;

public record OperationLogWrite(
        Long tenantId,
        Long projectId,
        String moduleCode,
        String actionCode,
        String objectType,
        Long objectId,
        Object beforeData,
        Object afterData,
        String reason
) {
}
