package com.yongquan.propertysaas.vehicle.domain;

import java.time.LocalDateTime;

public record VehicleModelView(
        Long modelId,
        Long brandId,
        String brandName,
        String modelName,
        Integer sortNo,
        String status,
        LocalDateTime createdAt
) {
}
