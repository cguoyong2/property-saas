package com.yongquan.propertysaas.fee.domain;

import java.math.BigDecimal;

public record BillStandardCandidate(
        Long projectId,
        Long itemId,
        Long standardId,
        String chargeMethod,
        BigDecimal unitPrice,
        String cycle,
        String formula,
        String objectType,
        Long objectId
) {
}
