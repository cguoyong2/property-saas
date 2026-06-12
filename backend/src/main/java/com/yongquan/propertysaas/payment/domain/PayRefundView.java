package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayRefundView(
        Long refundId,
        Long projectId,
        String refundNo,
        Long orderId,
        Long transactionId,
        BigDecimal refundAmount,
        String reason,
        String status,
        String thirdRefundNo,
        LocalDateTime refundedAt,
        Long applyUserId,
        Long auditUserId,
        LocalDateTime auditAt,
        LocalDateTime createdAt
) {
}
