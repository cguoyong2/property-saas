package com.yongquan.propertysaas.job.domain;

import java.time.LocalDateTime;

public record JobRunResult(
        String jobCode,
        boolean success,
        int affectedCount,
        String message,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
