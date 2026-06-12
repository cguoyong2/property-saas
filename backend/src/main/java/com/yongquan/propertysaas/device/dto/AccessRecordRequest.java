package com.yongquan.propertysaas.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AccessRecordRequest(
        @NotNull Long projectId,
        Long deviceId,
        Long memberId,
        Long visitorId,
        @NotBlank String openType,
        @NotBlank String openResult,
        @NotNull LocalDateTime occurredAt,
        String rawData
) {
}
