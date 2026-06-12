package com.yongquan.propertysaas.platform.dto;

public record TenantConfigRequest(
        String logoUrl,
        String servicePhone,
        String domain,
        String wechatAppid,
        String smsChannelCode,
        String storagePolicy
) {
}
