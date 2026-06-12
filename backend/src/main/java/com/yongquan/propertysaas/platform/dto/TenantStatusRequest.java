package com.yongquan.propertysaas.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantStatusRequest(
        @NotBlank String status,
        @NotBlank String reason
) {
}
