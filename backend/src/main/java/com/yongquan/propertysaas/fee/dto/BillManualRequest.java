package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BillManualRequest(
        @NotNull Long projectId,
        @NotNull Long itemId,
        Long standardId,
        @NotBlank String objectType,
        @NotNull Long objectId,
        Long memberId,
        Long houseId,
        @NotBlank String billPeriod,
        @NotNull BigDecimal receivableAmount,
        BigDecimal discountAmount,
        LocalDate dueDate
) {
}
