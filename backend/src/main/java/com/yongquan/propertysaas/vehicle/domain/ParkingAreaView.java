package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record ParkingAreaView(
        Long areaId,
        Long projectId,
        String areaName,
        String status,
        LocalDateTime createdAt
) {
}
