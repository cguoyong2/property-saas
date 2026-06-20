package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberRequest(
        String openid,
        String unionid,
        @NotBlank String mobile,
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
