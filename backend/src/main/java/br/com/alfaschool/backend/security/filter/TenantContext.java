package br.com.alfaschool.backend.security.filter;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    public static UUID getTenantId() {
        return TENANT_HOLDER.get();
    }

    public static void clear() {
        TENANT_HOLDER.remove();
    }
}
