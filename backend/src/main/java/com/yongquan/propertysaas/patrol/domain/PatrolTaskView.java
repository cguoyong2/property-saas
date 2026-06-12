package com.yongquan.propertysaas.patrol.domain;

import java.time.LocalDateTime;

public record PatrolTaskView(
        Long taskId,
        Long projectId,
        Long planId,
        String taskNo,
        String taskName,
        Long executorUserId,
        LocalDateTime plannedStartAt,
        LocalDateTime plannedEndAt,
        LocalDateTime actualStartAt,
        LocalDateTime actualEndAt,
        String status,
        LocalDateTime createdAt
) {
}
