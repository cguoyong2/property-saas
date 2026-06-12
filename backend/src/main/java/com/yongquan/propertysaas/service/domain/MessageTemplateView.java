package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record MessageTemplateView(
        Long templateId,
        Long tenantId,
        String templateCode,
        String templateName,
        String channel,
        String titleTemplate,
        String contentTemplate,
        String status,
        LocalDateTime createdAt
) {
}
