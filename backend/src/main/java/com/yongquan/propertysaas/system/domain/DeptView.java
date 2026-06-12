package com.yongquan.propertysaas.system.domain;

import java.time.LocalDateTime;

public record DeptView(
        Long deptId,
        Long parentId,
        String deptName,
        String deptType,
        Long projectId,
        Integer sortNo,
        String status,
        LocalDateTime createdAt
) {
}
