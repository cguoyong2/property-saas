package com.yongquan.propertysaas.device.adapter;

public record AdapterResult(
        boolean success,
        String requestUrl,
        String responseBody,
        String errorMessage,
        int costMs
) {
}
