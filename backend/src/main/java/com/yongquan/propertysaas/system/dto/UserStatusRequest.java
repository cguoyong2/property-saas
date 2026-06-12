package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStatusRequest(
        @NotBlank String status
) {
}
