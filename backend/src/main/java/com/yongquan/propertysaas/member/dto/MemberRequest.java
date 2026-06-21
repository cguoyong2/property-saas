package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MemberRequest(
        String openid,
        String unionid,
        @NotBlank
        @Pattern(regexp = "\\d{11}", message = "手机号必须为11位数字")
        String mobile,
        @NotBlank String realName,
        String avatarUrl,
        String status,
        @NotNull Long projectId,
        @NotNull Long buildingId,
        @NotNull Long unitId,
        @NotNull Long houseId,
        String bindRole
) {
}
