package com.yongquan.propertysaas.lease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LeaseCustomerRequest(
        @NotNull Long projectId,
        @NotBlank String customerName,
        String contactMobile,
        String sourceChannel,
        BigDecimal demandArea,
        BigDecimal budgetAmount,
        String status,
        Long ownerUserId
) {
}
