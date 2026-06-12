package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record NoticeView(
        Long noticeId,
        Long tenantId,
        Long projectId,
        String title,
        String content,
        String noticeType,
        String targetScope,
        String publishStatus,
        LocalDateTime publishedAt,
        Long publisherId,
        LocalDateTime createdAt
) {
}
