package com.yongquan.propertysaas.tenant.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property-saas.tenant")
public class TenantProperties {

    private String redisKeyPrefix = "tenant:{tenantId}:";
    private Set<String> ignoreTables = new LinkedHashSet<>();

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public Set<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
