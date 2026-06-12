package com.yongquan.propertysaas.device.domain;

import java.time.LocalDateTime;

public record AccessRecordView(
        Long recordId,
        Long projectId,
        Long deviceId,
        Long memberId,
        Long visitorId,
        String openType,
        String openResult,
        LocalDateTime occurredAt,
        String rawData
) {
}
