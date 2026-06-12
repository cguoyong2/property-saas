package com.yongquan.propertysaas.platform.domain;

public record MonitorAlertView(
        String metricCode,
        String level,
        long count,
        String message
) {
}
