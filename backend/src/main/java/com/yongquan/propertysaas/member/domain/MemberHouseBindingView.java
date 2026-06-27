package com.yongquan.propertysaas.member.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberHouseBindingView(
        Long bindId,
        Long projectId,
        String projectName,
        Long buildingId,
        String buildingName,
        Long unitId,
        String unitName,
        Long memberId,
        Long houseId,
        String houseNo,
        String roomNo,
        String bindRole,
        String realName,
        String mobile,
        String applySource,
        String status,
        LocalDate effectiveDate,
        LocalDate expireDate,
        Long auditUserId,
        LocalDateTime auditAt,
        String auditRemark,
        LocalDateTime createdAt
) {
}
