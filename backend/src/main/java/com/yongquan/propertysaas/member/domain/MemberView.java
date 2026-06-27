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
        String projectName,
        Long buildingId,
        String buildingName,
        Long unitId,
        String unitName,
        Long houseId,
        String houseNo,
        String roomNo,
        String bindRole,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
