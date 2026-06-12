package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record ParkingSyncRecordView(
        Long syncId,
        Long projectId,
        String vendorCode,
        String plateNo,
        String syncType,
        String syncStatus,
        String errorMessage,
        Integer retryCount,
        LocalDateTime createdAt
) {
}
