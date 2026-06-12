package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BillRemindRequest(
        @NotEmpty List<Long> billIds,
        String channel,
        String templateCode,
        String content
) {
}
