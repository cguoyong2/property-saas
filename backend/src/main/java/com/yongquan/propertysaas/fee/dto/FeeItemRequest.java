package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotBlank;

public record FeeItemRequest(
        String itemCode,
        @NotBlank String itemName,
        @NotBlank String itemType,
        String status
) {
}
