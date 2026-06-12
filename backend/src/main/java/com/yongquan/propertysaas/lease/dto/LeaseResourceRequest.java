package com.yongquan.propertysaas.lease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LeaseResourceRequest(
        @NotNull Long projectId,
        @NotBlank String resourceType,
        @NotBlank String resourceName,
        Long refObjectId,
        BigDecimal area,
        String status
) {
}
