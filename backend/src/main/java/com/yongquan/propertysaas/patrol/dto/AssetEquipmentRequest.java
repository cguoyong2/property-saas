package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetEquipmentRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 64) String equipmentCode,
        @NotBlank @Size(max = 100) String equipmentName,
        @NotBlank @Size(max = 64) String equipmentType,
        String location,
        Long responsibleUserId,
        String status
) {
}
