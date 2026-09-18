package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.estrutura.AccSala;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccSalaRepository extends JpaRepository<AccSala, UUID> {

    Page<AccSala> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<AccSala> findByTenantIdAndUnitIdAndDeletedFalse(UUID tenantId, UUID unitId, Pageable pageable);

    List<AccSala> findByTenantIdAndZonaIdAndDeletedFalse(UUID tenantId, UUID zonaId);

    Optional<AccSala> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    long countByTenantIdAndZonaIdAndDeletedFalse(UUID tenantId, UUID zonaId);
}
