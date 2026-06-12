package com.yongquan.propertysaas.security.domain;

import java.util.List;

public record CurrentUser(
        Long userId,
        Long tenantId,
        String username,
        String realName,
        String userType,
        String status,
        List<String> permissions
) {
}
