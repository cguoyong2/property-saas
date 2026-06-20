package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record ParkingSpaceView(
        Long spaceId,
        Long projectId,
        Long buildingId,
        Long unitId,
        Long houseId,
        Long areaId,
        String areaName,
        String spaceNo,
        String spaceType,
        String status,
        LocalDateTime createdAt
) {
}
