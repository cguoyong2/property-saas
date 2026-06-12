package com.yongquan.propertysaas.patrol.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatrolPlanView(
        Long planId,
        Long projectId,
        String planName,
        String cycleType,
        Long executorUserId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
}
