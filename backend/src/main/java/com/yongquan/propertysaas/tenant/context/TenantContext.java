package com.yongquan.propertysaas.tenant.context;

public final class TenantContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> PROJECT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_TYPE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long getProjectId() {
        return PROJECT_ID.get();
    }

    public static String getUserType() {
        return USER_TYPE.get();
    }

    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }

    public static boolean isPlatformUser() {
        return "PLATFORM".equals(USER_TYPE.get());
    }

    public static void set(Long tenantId, Long projectId) {
        TENANT_ID.set(tenantId);
        PROJECT_ID.set(projectId);
    }

    public static void set(Long userId, Long tenantId, Long projectId, String userType) {
        USER_ID.set(userId);
        TENANT_ID.set(tenantId);
        PROJECT_ID.set(projectId);
        USER_TYPE.set(userType);
    }

    public static Long requiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext missing tenant_id");
        }
        return tenantId;
    }

    public static void clear() {
        USER_ID.remove();
        TENANT_ID.remove();
        PROJECT_ID.remove();
        USER_TYPE.remove();
    }
}
