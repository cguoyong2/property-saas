package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkingSpaceRequest(
        @NotNull Long projectId,
        @NotNull Long buildingId,
        @NotNull Long unitId,
        @NotNull Long houseId,
        @NotNull Long areaId,
        @NotBlank String spaceNo,
        String spaceType,
        String status
) {
}
