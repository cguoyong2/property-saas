package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasonRequest(
        @NotBlank String reason
) {
}
