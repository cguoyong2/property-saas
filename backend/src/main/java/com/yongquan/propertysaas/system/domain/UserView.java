package com.yongquan.propertysaas.system.domain;

import java.time.LocalDateTime;
import java.util.List;

public record UserView(
        Long userId,
        Long deptId,
        String username,
        String realName,
        String mobile,
        String userType,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        List<Long> roleIds,
        List<Long> projectIds
) {
}
