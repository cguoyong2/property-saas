package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VehicleRequest(
        @NotNull Long projectId,
        @NotBlank String plateNo,
        String vehicleType,
        Long memberId,
        Long houseId,
        Long spaceId,
        String monthlyRentStatus,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
}
