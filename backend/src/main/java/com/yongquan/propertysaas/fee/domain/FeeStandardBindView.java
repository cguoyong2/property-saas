package com.yongquan.propertysaas.fee.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeStandardBindView(
        Long bindId,
        Long projectId,
        Long standardId,
        String objectType,
        Long objectId,
        LocalDate effectiveDate,
        LocalDate expireDate,
        String status,
        LocalDateTime createdAt
) {
}
