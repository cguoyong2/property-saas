package com.yongquan.propertysaas.member.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberHouseBindingView(
        Long bindId,
        Long projectId,
        Long memberId,
        Long houseId,
        String bindRole,
        String realName,
        String mobile,
        String status,
        LocalDate effectiveDate,
        LocalDate expireDate,
        Long auditUserId,
        LocalDateTime auditAt,
        String auditRemark,
        LocalDateTime createdAt
) {
}
