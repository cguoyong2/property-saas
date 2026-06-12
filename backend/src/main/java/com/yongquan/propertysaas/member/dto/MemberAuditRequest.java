package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberAuditRequest(
        @NotBlank String auditResult,
        String auditRemark
) {
}
