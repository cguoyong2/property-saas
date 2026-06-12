package com.yongquan.propertysaas.security.domain;

public record SysUserAccount(
        Long userId,
        Long tenantId,
        String username,
        String realName,
        String passwordHash,
        String userType,
        String status
) {
}
