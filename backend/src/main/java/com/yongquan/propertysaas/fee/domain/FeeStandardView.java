package com.yongquan.propertysaas.fee.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeStandardView(
        Long standardId,
        Long projectId,
        Long itemId,
        String standardName,
        String chargeMethod,
        BigDecimal unitPrice,
        String cycle,
        String formula,
        LocalDate effectiveDate,
        LocalDate expireDate,
        String status,
        LocalDateTime createdAt
) {
}
