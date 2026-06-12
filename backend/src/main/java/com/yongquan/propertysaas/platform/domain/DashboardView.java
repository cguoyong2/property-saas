package com.yongquan.propertysaas.platform.domain;

public record DashboardView(
        long tenantCount,
        long activeTenantCount,
        long packageCount
) {
}
