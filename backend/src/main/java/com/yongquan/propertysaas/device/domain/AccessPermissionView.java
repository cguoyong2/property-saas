package com.yongquan.propertysaas.device.domain;

import java.time.LocalDateTime;

public record AccessPermissionView(
        Long permissionId,
        Long projectId,
        Long memberId,
        Long userId,
        Long visitorId,
        Long deviceId,
        String deviceName,
        String vendorCode,
        String deviceCode,
        String permissionType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String syncStatus,
        LocalDateTime createdAt
) {
}
