package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest(
        String openid,
        String unionid,
        @NotBlank String mobile,
        @NotBlank String realName,
        String avatarUrl,
        String status
) {
}
