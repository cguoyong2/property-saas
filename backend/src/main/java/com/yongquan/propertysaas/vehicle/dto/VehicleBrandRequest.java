package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleBrandRequest(
        @NotBlank String brandName,
        String brandCode,
        Integer sortNo,
        String status
) {
}
