package com.yongquan.propertysaas.base.domain;

import java.time.LocalDateTime;

public record BuildingView(
        Long buildingId,
        Long projectId,
        String buildingName,
        String buildingCode,
        String buildingType,
        Integer floorCount,
        Integer sortNo,
        String status,
        LocalDateTime createdAt
) {
}
