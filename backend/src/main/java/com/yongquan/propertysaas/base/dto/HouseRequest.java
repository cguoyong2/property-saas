package com.yongquan.propertysaas.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HouseRequest(
        @NotNull Long projectId,
        @NotNull Long buildingId,
        Long unitId,
        @NotBlank String houseNo,
        Integer floorNo,
        BigDecimal buildingArea,
        BigDecimal innerArea,
        String houseUsage,
        String houseStatus,
        String chargeObject
) {
}
