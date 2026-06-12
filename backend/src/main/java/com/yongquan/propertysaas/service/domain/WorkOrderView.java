package com.yongquan.propertysaas.service.domain;

import java.time.LocalDateTime;

public record WorkOrderView(
        Long workOrderId,
        Long projectId,
        String orderNo,
        Long memberId,
        Long houseId,
        String orderType,
        String title,
        String description,
        String location,
        String imageFileIds,
        String priority,
        String status,
        Long acceptUserId,
        Long dispatchUserId,
        Long handlerUserId,
        LocalDateTime slaDeadline,
        LocalDateTime completedAt,
        LocalDateTime evaluatedAt,
        LocalDateTime createdAt
) {
}
