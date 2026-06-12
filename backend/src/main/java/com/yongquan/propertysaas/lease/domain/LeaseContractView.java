package com.yongquan.propertysaas.lease.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaseContractView(
        Long contractId,
        Long projectId,
        String contractNo,
        Long customerId,
        Long resourceId,
        String lesseeName,
        String lesseeMobile,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        BigDecimal depositAmount,
        String paymentCycle,
        Integer freeRentDays,
        String status,
        String attachmentFileIds,
        LocalDateTime createdAt
) {
}
