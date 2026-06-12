package com.yongquan.propertysaas.platform.domain;

import java.time.LocalDateTime;
import java.util.List;

public record PlatformMonitorView(
        LocalDateTime generatedAt,
        long interfaceFailureCount,
        long interfaceRetryPendingCount,
        long messageFailedCount,
        long messagePendingCount,
        long paymentFailedCount,
        long refundFailedCount,
        long loginFailedCount,
        long highRiskOperationCount,
        List<MonitorAlertView> alerts
) {
}
