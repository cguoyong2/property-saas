package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RefundCreateRequest(
        @NotNull Long projectId,
        @NotNull Long orderId,
        @NotNull BigDecimal refundAmount,
        @NotBlank String reason
) {
}
