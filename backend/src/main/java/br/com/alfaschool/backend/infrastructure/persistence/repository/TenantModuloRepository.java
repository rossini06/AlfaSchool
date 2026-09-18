package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.modulo.TenantModulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantModuloRepository extends JpaRepository<TenantModulo, UUID> {
    Optional<TenantModulo> findByTenantIdAndModuloCodigoAndDeletedFalse(UUID tenantId, String moduloCodigo);
    List<TenantModulo> findByTenantIdAndDeletedFalse(UUID tenantId);
}
