package com.yongquan.propertysaas.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundAuditRequest(
        @NotBlank String auditResult,
        String auditRemark
) {
}
