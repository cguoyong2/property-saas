package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record PayOrderCreateRequest(
        @NotNull Long projectId,
        @NotEmpty List<Long> billIds,
        @NotNull String payChannel,
        BigDecimal amount
) {
}
