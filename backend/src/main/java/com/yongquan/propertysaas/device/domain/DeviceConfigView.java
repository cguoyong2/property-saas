package com.yongquan.propertysaas.device.domain;

import java.time.LocalDateTime;

public record DeviceConfigView(
        Long deviceId,
        Long projectId,
        String deviceType,
        String vendorCode,
        String deviceCode,
        String deviceName,
        String location,
        String configJson,
        String status,
        LocalDateTime createdAt
) {
}
