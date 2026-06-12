package com.yongquan.propertysaas.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PackageRequest(
        @NotBlank String packageName,
        @NotBlank String packageCode,
        @NotNull BigDecimal price,
        @NotBlank String billingCycle,
        List<String> enabledModules,
        Map<String, Object> quotas
) {
}
