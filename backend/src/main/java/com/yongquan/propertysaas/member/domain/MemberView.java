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
        Long projectId,
        Long buildingId,
        Long unitId,
        Long houseId,
        String houseNo,
        String bindRole,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
