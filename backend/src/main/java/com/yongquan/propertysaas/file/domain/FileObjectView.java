package com.yongquan.propertysaas.file.domain;

import java.time.LocalDateTime;

public record FileObjectView(
        Long fileId,
        Long tenantId,
        Long projectId,
        String moduleCode,
        String originalName,
        String objectKey,
        String fileExt,
        String contentType,
        Long fileSize,
        Boolean sensitive,
        String uploaderType,
        Long uploaderId,
        LocalDateTime createdAt
) {
}
