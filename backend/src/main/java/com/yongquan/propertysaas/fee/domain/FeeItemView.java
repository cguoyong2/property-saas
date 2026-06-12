package com.yongquan.propertysaas.fee.domain;

import java.time.LocalDateTime;

public record FeeItemView(
        Long itemId,
        String itemCode,
        String itemName,
        String itemType,
        String status,
        LocalDateTime createdAt
) {
}
