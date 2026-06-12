package com.yongquan.propertysaas.lease.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record LeaseFollowRequest(
        @NotBlank String followType,
        @NotBlank String content,
        LocalDateTime nextFollowAt
) {
}
