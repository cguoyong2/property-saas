package com.yongquan.propertysaas.device.dto;

public record AccessSyncRequest(
        Long projectId,
        Long deviceId,
        Integer limit
) {
}
