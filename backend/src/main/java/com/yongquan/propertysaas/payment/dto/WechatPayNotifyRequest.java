package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record WechatPayNotifyRequest(
        @NotBlank String orderNo,
        @NotBlank String thirdTradeNo,
        @NotNull BigDecimal amount,
        @NotNull LocalDateTime paidAt,
        @NotBlank String signature,
        Map<String, Object> raw
) {
}
