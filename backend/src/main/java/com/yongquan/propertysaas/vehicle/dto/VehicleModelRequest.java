package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleModelRequest(
        @NotNull Long brandId,
        @NotBlank String modelName,
        Integer sortNo,
        String status
) {
}
