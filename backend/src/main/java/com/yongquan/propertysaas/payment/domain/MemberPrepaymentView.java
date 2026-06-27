package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberPrepaymentView(
        Long prepaymentId,
        Long projectId,
        String projectName,
        Long memberId,
        String memberName,
        String mobile,
        Long orderId,
        String orderNo,
        BigDecimal amount,
        BigDecimal usedAmount,
        BigDecimal refundedAmount,
        BigDecimal remainingAmount,
        String source,
        String remark,
        String houseNo,
        String billSummary,
        LocalDateTime createdAt
) {
}
