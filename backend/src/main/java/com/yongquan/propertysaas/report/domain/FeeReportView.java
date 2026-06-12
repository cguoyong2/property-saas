package com.yongquan.propertysaas.report.domain;

import java.math.BigDecimal;

public record FeeReportView(
        BigDecimal receivableAmount,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal netPaidAmount,
        BigDecimal arrearsAmount,
        BigDecimal collectionRate,
        long billCount,
        long unpaidBillCount,
        long paidBillCount
) {
}
