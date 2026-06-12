package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
        @NotBlank String roleName,
        @NotBlank String roleCode,
        String roleLevel,
        String dataScope,
        String status
) {
}
