package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record ParkingSpaceView(
        Long spaceId,
        Long projectId,
        String spaceNo,
        String spaceType,
        String status,
        Long houseId,
        LocalDateTime createdAt
) {
}
