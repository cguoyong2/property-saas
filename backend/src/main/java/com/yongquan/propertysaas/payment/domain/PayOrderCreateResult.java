package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayOrderCreateResult(
        Long orderId,
        String orderNo,
        BigDecimal amount,
        String status,
        String prepayId,
        LocalDateTime expireAt
) {
}
