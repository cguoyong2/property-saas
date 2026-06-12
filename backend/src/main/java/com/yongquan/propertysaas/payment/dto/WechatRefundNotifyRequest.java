package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record WechatRefundNotifyRequest(
        @NotBlank String refundNo,
        @NotBlank String thirdRefundNo,
        @NotNull BigDecimal refundAmount,
        @NotNull LocalDateTime refundedAt,
        @NotBlank String signature,
        Map<String, Object> raw
) {
}
