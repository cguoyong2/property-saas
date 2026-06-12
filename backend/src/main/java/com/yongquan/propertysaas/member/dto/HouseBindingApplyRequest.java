package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HouseBindingApplyRequest(
        @NotNull Long tenantId,
        @NotNull Long projectId,
        @NotNull Long memberId,
        @NotNull Long houseId,
        @NotBlank String bindRole,
        @NotBlank String realName,
        @NotBlank String mobile,
        String idCardNoEncrypted,
        String proofFileIds,
        LocalDate effectiveDate,
        LocalDate expireDate
) {
}
