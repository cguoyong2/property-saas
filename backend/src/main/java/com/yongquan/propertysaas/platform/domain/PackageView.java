package com.yongquan.propertysaas.platform.domain;

import java.math.BigDecimal;

public record PackageView(
        Long packageId,
        String packageName,
        String packageCode,
        BigDecimal price,
        String billingCycle,
        String enabledModules,
        String quotas,
        String status
) {
}
