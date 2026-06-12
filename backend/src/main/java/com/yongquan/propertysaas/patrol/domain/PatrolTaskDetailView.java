package com.yongquan.propertysaas.patrol.domain;

import java.util.List;

public record PatrolTaskDetailView(
        PatrolTaskView task,
        List<PatrolTaskItemView> items
) {
}
