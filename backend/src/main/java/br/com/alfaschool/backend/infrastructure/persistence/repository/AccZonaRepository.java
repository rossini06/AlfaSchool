package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.estrutura.AccZona;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccZonaRepository extends JpaRepository<AccZona, UUID> {

    Page<AccZona> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<AccZona> findByTenantIdAndUnitIdAndDeletedFalse(UUID tenantId, UUID unitId, Pageable pageable);

    List<AccZona> findByTenantIdAndUnitIdAndDeletedFalse(UUID tenantId, UUID unitId);

    Optional<AccZona> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    boolean existsByTenantIdAndUnitIdAndNomeIgnoreCaseAndDeletedFalse(UUID tenantId, UUID unitId, String nome);
}
