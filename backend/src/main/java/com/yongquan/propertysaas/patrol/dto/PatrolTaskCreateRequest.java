package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record PatrolTaskCreateRequest(
        @NotNull Long projectId,
        Long planId,
        @NotBlank String taskName,
        Long executorUserId,
        @NotNull LocalDateTime plannedStartAt,
        @NotNull LocalDateTime plannedEndAt,
        @NotEmpty List<Long> pointIds
) {
}
