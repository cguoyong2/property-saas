package com.yongquan.propertysaas.fee.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeBillView(
        Long billId,
        Long projectId,
        String billNo,
        Long itemId,
        Long standardId,
        String objectType,
        Long objectId,
        Long memberId,
        Long houseId,
        String billPeriod,
        BigDecimal receivableAmount,
        BigDecimal discountAmount,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        String status,
        String sourceType,
        String voidReason,
        LocalDateTime createdAt
) {
}
