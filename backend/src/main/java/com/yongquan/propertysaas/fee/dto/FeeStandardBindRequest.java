package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FeeStandardBindRequest(
        @NotNull Long projectId,
        @NotNull Long standardId,
        @NotBlank String objectType,
        @NotNull Long objectId,
        @NotNull LocalDate effectiveDate,
        LocalDate expireDate,
        String status
) {
}
