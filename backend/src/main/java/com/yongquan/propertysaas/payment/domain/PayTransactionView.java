package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayTransactionView(
        Long transactionId,
        Long projectId,
        Long orderId,
        String orderNo,
        String thirdTradeNo,
        String payChannel,
        BigDecimal amount,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        Long memberId,
        String memberName,
        String memberMobile,
        String orderStatus,
        BigDecimal orderAmount,
        BigDecimal prepaymentAmount
) {
}
