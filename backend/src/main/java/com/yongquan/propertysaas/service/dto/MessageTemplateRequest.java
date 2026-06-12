package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageTemplateRequest(
        @NotBlank @Size(max = 80) String templateCode,
        @NotBlank @Size(max = 100) String templateName,
        @NotBlank @Size(max = 32) String channel,
        @Size(max = 200) String titleTemplate,
        @NotBlank String contentTemplate,
        @Size(max = 32) String status
) {
}
