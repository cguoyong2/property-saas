package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkingSpaceRequest(
        @NotNull Long projectId,
        @NotBlank String spaceNo,
        String spaceType,
        String status,
        Long houseId
) {
}
