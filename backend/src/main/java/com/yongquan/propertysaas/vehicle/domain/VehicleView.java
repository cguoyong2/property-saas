package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VehicleView(
        Long vehicleId,
        Long projectId,
        String projectName,
        Long buildingId,
        String buildingName,
        Long unitId,
        String unitName,
        String houseNo,
        String plateNo,
        String vehicleType,
        String vehicleBrand,
        String vehicleModel,
        Long memberId,
        String memberName,
        String memberMobile,
        Long houseId,
        Long spaceId,
        String areaName,
        String spaceNo,
        String monthlyRentStatus,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
}
