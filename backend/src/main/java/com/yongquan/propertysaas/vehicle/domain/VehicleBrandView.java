package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record VehicleBrandView(
        Long brandId,
        String brandName,
        String brandCode,
        Integer sortNo,
        String status,
        LocalDateTime createdAt
) {
}
