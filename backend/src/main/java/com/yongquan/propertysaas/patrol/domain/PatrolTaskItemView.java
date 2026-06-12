package com.yongquan.propertysaas.patrol.domain;

import java.time.LocalDateTime;

public record PatrolTaskItemView(
        Long itemId,
        Long taskId,
        Long pointId,
        String result,
        String content,
        String imageFileIds,
        LocalDateTime checkedAt,
        String status,
        LocalDateTime createdAt
) {
}
