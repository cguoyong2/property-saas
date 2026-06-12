package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PatrolPlanRequest(
        @NotNull Long projectId,
        @NotBlank String planName,
        @NotBlank String cycleType,
        Long executorUserId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        String status
) {
}
