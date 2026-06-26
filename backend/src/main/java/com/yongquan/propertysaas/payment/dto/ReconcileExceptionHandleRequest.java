package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ReconcileExceptionHandleRequest(
        @NotBlank String handleRemark
) {
}
