package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.jornada.AccJornadaDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccJornadaDiaRepository extends JpaRepository<AccJornadaDia, UUID> {

    List<AccJornadaDia> findByTenantIdAndJornadaIdAndDeletedFalseOrderByDiaSemanaAsc(UUID tenantId, UUID jornadaId);

    Optional<AccJornadaDia> findByTenantIdAndJornadaIdAndDiaSemanaAndDeletedFalse(UUID tenantId, UUID jornadaId, int diaSemana);

    List<AccJornadaDia> findByTenantIdAndJornadaIdInAndDeletedFalse(UUID tenantId, List<UUID> jornadaIds);
}
