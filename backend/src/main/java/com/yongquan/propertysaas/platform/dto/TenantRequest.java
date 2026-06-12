package com.yongquan.propertysaas.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TenantRequest(
        @NotBlank String tenantName,
        @NotBlank String tenantCode,
        String unifiedSocialCreditCode,
        @NotBlank String contactName,
        @NotBlank String contactMobile,
        @NotNull Long packageId,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        String remark
) {
}
