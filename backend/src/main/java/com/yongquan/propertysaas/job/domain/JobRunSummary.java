package com.yongquan.propertysaas.job.domain;

import java.time.LocalDateTime;
import java.util.List;

public record JobRunSummary(
        String scope,
        int tenantCount,
        int affectedCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<JobRunResult> results
) {
}
