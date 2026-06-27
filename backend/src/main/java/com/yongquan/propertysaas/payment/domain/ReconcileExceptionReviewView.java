package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReconcileExceptionReviewView(
        String exceptionKey,
        Long projectId,
        String exceptionType,
        String exceptionLevel,
        String businessType,
        Long businessId,
        String businessNo,
        String memberName,
        String memberMobile,
        BigDecimal amount,
        String reason,
        String status,
        LocalDateTime handledAt,
        Long handledBy,
        String handleRemark,
        String attachmentFileIds,
        String reviewStatus,
        LocalDateTime reviewedAt,
        Long reviewedBy,
        String reviewRemark,
        String currentCheckStatus,
        LocalDateTime createdAt
) {
}
