package com.yongquan.propertysaas.report.domain;

import java.math.BigDecimal;

public record LeaseReportView(
        BigDecimal rentableArea,
        BigDecimal leasedArea,
        BigDecimal occupancyRate,
        long vacantResourceCount,
        long activeContractCount,
        long expiringContractCount,
        BigDecimal rentReceivableAmount,
        BigDecimal rentPaidAmount,
        long customerCount,
        long signedCustomerCount,
        BigDecimal customerConversionRate
) {
}
