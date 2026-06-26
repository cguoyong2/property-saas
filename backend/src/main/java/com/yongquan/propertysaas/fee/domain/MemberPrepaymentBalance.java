package com.yongquan.propertysaas.fee.domain;

import java.math.BigDecimal;

public record MemberPrepaymentBalance(
        Long prepaymentId,
        BigDecimal remainingAmount
) {
}
