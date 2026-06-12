package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;

public record PayableBill(
        Long billId,
        Long projectId,
        String billNo,
        Long memberId,
        BigDecimal remainingAmount,
        String status
) {
}
