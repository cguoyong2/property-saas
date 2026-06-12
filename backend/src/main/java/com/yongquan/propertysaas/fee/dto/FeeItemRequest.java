package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;

public record FeeItemRequest(
        @NotBlank String itemCode,
        @NotBlank String itemName,
        @NotBlank String itemType,
        String status
) {
}
