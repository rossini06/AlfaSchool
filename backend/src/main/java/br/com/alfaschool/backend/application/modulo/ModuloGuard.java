package br.com.alfaschool.backend.application.modulo;

import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantModuloRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guard de modulo contratado, usado em @PreAuthorize:
 *
 *   @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
 *
 * Fail-closed: sem tenant no contexto, nega. Assim um endpoint novo nao
 * nasce acessivel para quem nao contratou o modulo.
 */
@Component("moduloGuard")
public class ModuloGuard {

    private final TenantModuloRepository tenantModuloRepository;

    public ModuloGuard(TenantModuloRepository tenantModuloRepository) {
        this.tenantModuloRepository = tenantModuloRepository;
    }

    public boolean has(String codigo) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return false;
        }
        return tenantModuloRepository
                .findByTenantIdAndModuloCodigoAndDeletedFalse(tenantId, codigo)
                .map(tm -> tm.vigente())
                .orElse(false);
    }
}
