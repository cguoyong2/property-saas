package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record MessageRecordView(
        Long messageId,
        Long tenantId,
        Long projectId,
        String receiverType,
        Long receiverId,
        String receiverMobile,
        String channel,
        String templateCode,
        String title,
        String content,
        String sendStatus,
        String readStatus,
        LocalDateTime readAt,
        String failReason,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
