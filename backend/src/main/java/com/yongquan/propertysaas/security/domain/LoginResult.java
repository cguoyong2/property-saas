package com.yongquan.propertysaas.security.domain;

public record LoginResult(
        String token,
        Long userId,
        Long tenantId,
        String userType,
        java.util.List<String> permissions
) {
}
