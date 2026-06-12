package com.yongquan.propertysaas.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceConfigRequest(
        @NotNull Long projectId,
        @NotBlank String deviceType,
        @NotBlank String vendorCode,
        @NotBlank String deviceCode,
        @NotBlank String deviceName,
        String location,
        String configJson,
        String status
) {
}
