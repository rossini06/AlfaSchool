package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.permanencia.AccPresencaPar;
import br.com.alfaschool.backend.domain.access.permanencia.OrigemPar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccPresencaParRepository extends JpaRepository<AccPresencaPar, UUID> {

    List<AccPresencaPar> findByTenantIdAndPresencaIdOrderByEntradaEmAsc(UUID tenantId, UUID presencaId);

    List<AccPresencaPar> findByTenantIdAndPresencaIdInOrderByEntradaEmAsc(UUID tenantId, List<UUID> presencaIds);

    Optional<AccPresencaPar> findByIdAndTenantId(UUID id, UUID tenantId);

    /** O recalculo apaga so' o que ele mesmo criou; par MANUAL sobrevive. */
    void deleteByTenantIdAndPresencaIdAndOrigem(UUID tenantId, UUID presencaId, OrigemPar origem);

    long countByTenantIdAndPresencaIdAndOrigem(UUID tenantId, UUID presencaId, OrigemPar origem);
}
