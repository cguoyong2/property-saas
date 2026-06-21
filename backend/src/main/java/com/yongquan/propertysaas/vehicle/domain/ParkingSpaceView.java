package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record ParkingSpaceView(
        Long spaceId,
        Long projectId,
        String projectName,
        Long buildingId,
        String buildingName,
        Long unitId,
        String unitName,
        Long houseId,
        String houseNo,
        Long areaId,
        String areaName,
        String spaceNo,
        String spaceType,
        String status,
        Long boundVehicleId,
        String boundPlateNo,
        String boundVehicleBrand,
        String boundVehicleModel,
        String monthlyRentStatus,
        Long boundMemberId,
        String boundMemberName,
        String boundMemberMobile,
        LocalDateTime createdAt
) {
}
