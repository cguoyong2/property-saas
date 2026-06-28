package com.yongquan.propertysaas.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record VisitorInviteRequest(
        @NotNull Long projectId,
        Long inviterMemberId,
        Long houseId,
        @NotBlank String visitorName,
        String visitorMobile,
        String visitReason,
        @NotNull LocalDateTime validStartAt,
        @NotNull LocalDateTime validEndAt,
        List<Long> deviceIds
) {
}
