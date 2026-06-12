package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayOrderView(
        Long orderId,
        Long tenantId,
        Long projectId,
        String orderNo,
        Long memberId,
        String payChannel,
        BigDecimal amount,
        String subject,
        String status,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        String thirdTradeNo,
        LocalDateTime createdAt
) {
}
