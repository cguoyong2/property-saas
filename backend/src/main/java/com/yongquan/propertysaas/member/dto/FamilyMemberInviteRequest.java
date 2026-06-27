package com.yongquan.propertysaas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FamilyMemberInviteRequest(
        @NotNull Long houseId,
        @NotBlank String realName,
        @NotBlank String mobile,
        @NotBlank String bindRole,
        String relationship,
        Boolean allowNotice,
        Boolean allowBill,
        Boolean allowPayment,
        Boolean allowWorkOrder,
        Boolean allowVisitor
) {
}
