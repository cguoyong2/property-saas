package com.yongquan.propertysaas.lease.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeaseResourceView(
        Long resourceId,
        Long projectId,
        String resourceType,
        String resourceName,
        Long refObjectId,
        BigDecimal area,
        String status,
        LocalDateTime createdAt
) {
}
