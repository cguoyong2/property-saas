package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;

public record OrderBillSettlement(
        Long billId,
        Long memberId,
        BigDecimal allocatedAmount,
        BigDecimal currentRemainingAmount
) {
}
