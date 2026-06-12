package com.yongquan.propertysaas.payment.domain;

public record PaymentNotifyResult(
        String orderNo,
        String status,
        boolean idempotent
) {
}
