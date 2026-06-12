package com.yongquan.propertysaas.platform.domain;

public record TenantConfigView(
        Long configId,
        Long tenantId,
        String logoUrl,
        String servicePhone,
        String domain,
        String wechatAppid,
        String smsChannelCode,
        String storagePolicy
) {
}
