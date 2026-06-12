package com.yongquan.propertysaas.system.domain;

import java.time.LocalDateTime;

public record RoleView(
        Long roleId,
        String roleName,
        String roleCode,
        String roleLevel,
        String dataScope,
        String status,
        LocalDateTime createdAt
) {
}
