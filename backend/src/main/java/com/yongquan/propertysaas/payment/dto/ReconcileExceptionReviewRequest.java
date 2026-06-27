package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ReconcileExceptionReviewRequest(
        @NotBlank String reviewResult,
        String reviewRemark
) {
}
