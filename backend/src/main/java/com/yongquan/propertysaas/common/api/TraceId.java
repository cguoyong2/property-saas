package com.yongquan.propertysaas.common.api;

import java.util.UUID;

public final class TraceId {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceId() {
    }

    public static String current() {
        String traceId = HOLDER.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void set(String traceId) {
        HOLDER.set(traceId);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
