package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VehicleView(
        Long vehicleId,
        Long projectId,
        String plateNo,
        String vehicleType,
        Long memberId,
        Long houseId,
        Long spaceId,
        String monthlyRentStatus,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
}
