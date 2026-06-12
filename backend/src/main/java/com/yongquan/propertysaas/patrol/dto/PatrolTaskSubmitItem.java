package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PatrolTaskSubmitItem(
        @NotNull Long itemId,
        @NotBlank String result,
        String content,
        String imageFileIds
) {
}
