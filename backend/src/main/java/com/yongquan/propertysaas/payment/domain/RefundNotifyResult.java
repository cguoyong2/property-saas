package com.yongquan.propertysaas.payment.domain;

public record RefundNotifyResult(
        String refundNo,
        String status,
        boolean idempotent
) {
}
