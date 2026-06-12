package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlatformNoticeCreateRequest(
        @NotNull Long tenantId,
        Long projectId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        String noticeType,
        String targetScope,
        List<String> channels,
        String templateCode,
        Boolean publishNow
) {
}
