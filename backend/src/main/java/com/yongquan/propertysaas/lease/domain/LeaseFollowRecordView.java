package com.yongquan.propertysaas.lease.domain;

import java.time.LocalDateTime;

public record LeaseFollowRecordView(
        Long followId,
        Long customerId,
        String followType,
        String content,
        LocalDateTime nextFollowAt,
        Long createdBy,
        LocalDateTime createdAt
) {
}
