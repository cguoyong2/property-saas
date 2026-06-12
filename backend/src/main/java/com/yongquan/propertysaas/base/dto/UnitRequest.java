package com.yongquan.propertysaas.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UnitRequest(
        @NotNull Long projectId,
        @NotNull Long buildingId,
        @NotBlank String unitName,
        Integer sortNo,
        String status
) {
}
