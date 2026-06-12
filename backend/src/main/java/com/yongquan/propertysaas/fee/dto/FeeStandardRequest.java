package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeStandardRequest(
        Long projectId,
        @NotNull Long itemId,
        @NotBlank String standardName,
        @NotBlank String chargeMethod,
        BigDecimal unitPrice,
        String cycle,
        String formula,
        @NotNull LocalDate effectiveDate,
        LocalDate expireDate,
        String status
) {
}
