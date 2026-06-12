package com.yongquan.propertysaas.service.domain;

import java.util.List;

public record WorkOrderDetailView(
        WorkOrderView workOrder,
        List<WorkOrderEventView> events,
        List<WorkOrderCommentView> comments
) {
}
