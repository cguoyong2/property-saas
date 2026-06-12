package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record WorkOrderCommentView(
        Long commentId,
        Long workOrderId,
        Long memberId,
        Integer score,
        String content,
        LocalDateTime createdAt
) {
}
