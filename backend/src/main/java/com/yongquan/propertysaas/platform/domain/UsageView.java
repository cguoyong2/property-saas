package com.yongquan.propertysaas.platform.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageView(
        Long tenantId,
        LocalDate statDate,
        Integer projectCount,
        Integer userCount,
        Integer memberCount,
        Integer houseCount,
        BigDecimal storageMb,
        Integer smsCount,
        Integer apiCallCount
) {
}
