package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record WorkOrderEventView(
        Long eventId,
        Long workOrderId,
        String fromStatus,
        String toStatus,
        String action,
        String operatorType,
        Long operatorId,
        String content,
        String imageFileIds,
        LocalDateTime createdAt
) {
}
