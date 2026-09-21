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

    /**
     * Executa sem o filtro de tenant e restaura o contexto ao sair.
     *
     * Existe para a administracao da plataforma: o superadministrador vive
     * no tenant mestre, e com o filtro ligado a listagem de redes devolvia
     * so' o mestre — a tabela de tenants tambem tem tenant_id. Use apenas
     * em servico que e', por definicao, cross-tenant (TenantService,
     * SaasService); em qualquer outro lugar isto e' um vazamento.
     */
    public static <T> T semFiltro(java.util.function.Supplier<T> acao) {
        UUID anterior = TENANT_HOLDER.get();
        TENANT_HOLDER.remove();
        try {
            return acao.get();
        } finally {
            if (anterior != null) {
                TENANT_HOLDER.set(anterior);
            }
        }
    }
}
