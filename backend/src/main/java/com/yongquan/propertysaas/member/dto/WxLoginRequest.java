package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WxLoginRequest(
        @NotNull Long tenantId,
        @NotBlank String openid,
        String unionid,
        String mobile,
        String realName,
        String avatarUrl
) {
}
