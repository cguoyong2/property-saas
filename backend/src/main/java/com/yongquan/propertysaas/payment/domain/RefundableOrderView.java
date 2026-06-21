package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundableOrderView(
        Long orderId,
        String orderNo,
        Long projectId,
        Long memberId,
        String memberName,
        String mobile,
        String houseNo,
        BigDecimal amount,
        BigDecimal refundedAmount,
        BigDecimal refundableAmount,
        String billSummary,
        LocalDateTime paidAt
) {
}
