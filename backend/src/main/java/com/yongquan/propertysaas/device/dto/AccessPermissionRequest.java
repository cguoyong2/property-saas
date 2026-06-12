package com.yongquan.propertysaas.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AccessPermissionRequest(
        @NotNull Long projectId,
        Long memberId,
        Long userId,
        Long visitorId,
        @NotNull Long deviceId,
        @NotBlank String permissionType,
        @NotNull LocalDateTime startAt,
        LocalDateTime endAt,
        String status
) {
}
