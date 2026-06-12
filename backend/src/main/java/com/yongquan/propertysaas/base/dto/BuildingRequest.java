package com.yongquan.propertysaas.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(
        @NotNull Long projectId,
        @NotBlank String buildingName,
        String buildingCode,
        String buildingType,
        Integer floorCount,
        Integer sortNo,
        String status
) {
}
