package com.yongquan.propertysaas.base.domain;

import java.time.LocalDateTime;

public record UnitView(
        Long unitId,
        Long projectId,
        Long buildingId,
        String unitName,
        Integer sortNo,
        String status,
        LocalDateTime createdAt
) {
}
