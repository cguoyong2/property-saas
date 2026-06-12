package com.yongquan.propertysaas.file.domain;

public record FileUploadResult(
        Long fileId,
        String originalName,
        String moduleCode,
        Long projectId,
        String contentType,
        Long fileSize,
        String downloadUrl
) {
}
