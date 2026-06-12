package com.yongquan.propertysaas.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property-saas.job")
public class JobProperties {

    private boolean enabled = false;
    private int defaultLimit = 200;
    private int leaseExpireDays = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getLeaseExpireDays() {
        return leaseExpireDays;
    }

    public void setLeaseExpireDays(int leaseExpireDays) {
        this.leaseExpireDays = leaseExpireDays;
    }
}
