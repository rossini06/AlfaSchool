package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.jornada.AccJornada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccJornadaRepository extends JpaRepository<AccJornada, UUID> {

    Page<AccJornada> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<AccJornada> findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(UUID tenantId, String nome, Pageable pageable);

    Optional<AccJornada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<AccJornada> findByTenantIdAndIdInAndDeletedFalse(UUID tenantId, List<UUID> ids);

    boolean existsByTenantIdAndNomeIgnoreCaseAndDeletedFalse(UUID tenantId, String nome);
}
