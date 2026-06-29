package com.sambath.admincafe.tenant;

public final class TenantContext {

    public static final Long SYSTEM = -1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static Long require() {
        Long id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("Tenant context is not set for this request.");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
