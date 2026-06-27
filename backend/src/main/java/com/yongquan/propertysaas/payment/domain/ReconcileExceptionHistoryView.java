package com.yongquan.propertysaas.payment.domain;

import java.time.LocalDateTime;

public record ReconcileExceptionHistoryView(
        Long historyId,
        String exceptionKey,
        String actionType,
        String beforeStatus,
        String afterStatus,
        String beforeReviewStatus,
        String afterReviewStatus,
        String remark,
        String attachmentFileIds,
        Long operatorId,
        LocalDateTime createdAt
) {
}
