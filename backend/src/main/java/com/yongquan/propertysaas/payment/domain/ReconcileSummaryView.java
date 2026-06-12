package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;

public record ReconcileSummaryView(
        Long projectId,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        BigDecimal orderPaidAmount,
        BigDecimal exceptionAmount
) {
}
