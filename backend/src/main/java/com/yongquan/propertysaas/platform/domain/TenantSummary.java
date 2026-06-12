package com.yongquan.propertysaas.platform.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantSummary(
        Long tenantId,
        String tenantName,
        String tenantCode,
        String contactName,
        String contactMobile,
        Long packageId,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        String status,
        LocalDateTime createdAt
) {
}
