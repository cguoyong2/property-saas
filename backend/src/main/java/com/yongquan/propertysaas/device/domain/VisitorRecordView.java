package com.yongquan.propertysaas.device.domain;

import java.time.LocalDateTime;

public record VisitorRecordView(
        Long visitorId,
        Long projectId,
        Long inviterMemberId,
        String visitorName,
        String visitorMobile,
        String visitReason,
        LocalDateTime validStartAt,
        LocalDateTime validEndAt,
        String qrCode,
        String status,
        LocalDateTime createdAt
) {
}
