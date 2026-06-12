package com.yongquan.propertysaas.patrol.domain;

import java.time.LocalDateTime;

public record AssetEquipmentView(
        Long equipmentId,
        Long projectId,
        String equipmentCode,
        String equipmentName,
        String equipmentType,
        String location,
        Long responsibleUserId,
        String status,
        LocalDateTime createdAt
) {
}
