package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WorkOrderEvaluateRequest(
        @NotNull Long memberId,
        @NotNull @Min(1) @Max(5) Integer score,
        String content
) {
}
