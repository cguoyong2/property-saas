package com.yongquan.propertysaas.member.domain;

import java.time.LocalDateTime;

public record MemberView(
        Long memberId,
        String openid,
        String unionid,
        String mobile,
        String realName,
        String avatarUrl,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
