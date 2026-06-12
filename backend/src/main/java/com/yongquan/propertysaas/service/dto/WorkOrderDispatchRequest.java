package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.NotNull;

public record WorkOrderDispatchRequest(
        @NotNull Long handlerUserId,
        String content
) {
}
