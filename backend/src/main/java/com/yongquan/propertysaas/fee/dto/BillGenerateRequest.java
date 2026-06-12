package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BillGenerateRequest(
        @NotNull Long projectId,
        @NotNull Long itemId,
        @NotBlank String billPeriod,
        String objectType,
        List<Long> objectIds,
        LocalDate dueDate
) {
}
