package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;

public record ReconcileSummaryView(
        Long projectId,
        Long transactionCount,
        Long refundCount,
        Long orderCount,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal prepaymentAmount,
        BigDecimal prepaymentUsedAmount,
        BigDecimal billAppliedAmount,
        BigDecimal netAmount,
        BigDecimal orderPaidAmount,
        BigDecimal exceptionAmount,
        Long exceptionCount
) {
}
