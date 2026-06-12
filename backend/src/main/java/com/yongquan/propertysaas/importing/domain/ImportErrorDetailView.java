package com.yongquan.propertysaas.importing.domain;

import java.time.LocalDateTime;

public record ImportErrorDetailView(
        Long errorId,
        Long batchId,
        Integer rowNo,
        String fieldName,
        String rawValue,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt
) {
}
