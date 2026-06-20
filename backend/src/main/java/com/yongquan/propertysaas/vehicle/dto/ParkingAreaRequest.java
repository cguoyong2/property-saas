package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkingAreaRequest(
        @NotNull Long projectId,
        @NotBlank String areaName,
        String status
) {
}
