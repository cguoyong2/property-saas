package com.yongquan.propertysaas.lease.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeaseCustomerView(
        Long customerId,
        Long projectId,
        String customerName,
        String contactMobile,
        String sourceChannel,
        BigDecimal demandArea,
        BigDecimal budgetAmount,
        String status,
        Long ownerUserId,
        LocalDateTime createdAt
) {
}
