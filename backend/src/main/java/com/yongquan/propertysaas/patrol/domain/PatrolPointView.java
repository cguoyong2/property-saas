package com.yongquan.propertysaas.patrol.domain;

import java.time.LocalDateTime;

public record PatrolPointView(
        Long pointId,
        Long projectId,
        String pointCode,
        String pointName,
        String pointType,
        Long equipmentId,
        String location,
        String qrCode,
        String nfcCode,
        String status,
        LocalDateTime createdAt
) {
}
