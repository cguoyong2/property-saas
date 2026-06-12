package com.yongquan.propertysaas.lease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaseContractRequest(
        @NotNull Long projectId,
        Long customerId,
        @NotNull Long resourceId,
        @NotBlank String lesseeName,
        String lesseeMobile,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull BigDecimal rentAmount,
        BigDecimal depositAmount,
        String paymentCycle,
        Integer freeRentDays,
        String attachmentFileIds,
        Boolean activeNow
) {
}
