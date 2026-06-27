package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record HouseBindingApplyRequest(
        @NotNull Long tenantId,
        @NotNull Long projectId,
        @NotNull Long memberId,
        @NotNull Long houseId,
        @NotBlank String bindRole,
        @NotBlank String realName,
        @Pattern(regexp = "\\d{11}", message = "手机号必须为11位数字")
        @NotBlank String mobile,
        String idCardNoEncrypted,
        String proofFileIds,
        LocalDate effectiveDate,
        LocalDate expireDate
) {
}
