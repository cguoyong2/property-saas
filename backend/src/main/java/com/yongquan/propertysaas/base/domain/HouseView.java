package com.yongquan.propertysaas.base.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HouseView(
        Long houseId,
        Long projectId,
        Long buildingId,
        Long unitId,
        String houseNo,
        Integer floorNo,
        BigDecimal buildingArea,
        BigDecimal innerArea,
        String houseUsage,
        String houseStatus,
        String chargeObject,
        LocalDateTime createdAt
) {
}
