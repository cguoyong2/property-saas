package com.yongquan.propertysaas.tenant.redis;

import com.yongquan.propertysaas.tenant.config.TenantProperties;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class TenantRedisKeyBuilder {

    private final TenantProperties tenantProperties;

    public TenantRedisKeyBuilder(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    public String build(String key) {
        return build(TenantContext.requiredTenantId(), key);
    }

    public String build(Long tenantId, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key suffix must not be blank");
        }
        return tenantProperties.getRedisKeyPrefix().replace("{tenantId}", String.valueOf(tenantId)) + key;
    }
}
