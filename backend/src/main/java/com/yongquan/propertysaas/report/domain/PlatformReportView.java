package com.yongquan.propertysaas.report.domain;

public record PlatformReportView(
        long tenantCount,
        long activeTenantCount,
        long expiringTenantCount,
        long projectCount,
        long houseCount,
        long userCount,
        long memberCount,
        long interfaceFailureCount
) {
}
