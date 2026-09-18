package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.calendario.AccCalendarioDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccCalendarioDiaRepository extends JpaRepository<AccCalendarioDia, UUID> {

    Optional<AccCalendarioDia> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * Ignora o soft delete de proposito: a UNIQUE (calendario_id, data) do banco
     * nao ignora, entao regravar um dia "excluido" precisa reaproveitar a linha
     * em vez de inserir outra e estourar violacao de chave.
     */
    Optional<AccCalendarioDia> findByCalendarioIdAndData(UUID calendarioId, LocalDate data);

    List<AccCalendarioDia> findByTenantIdAndCalendarioIdAndDeletedFalse(UUID tenantId, UUID calendarioId);

    List<AccCalendarioDia> findByTenantIdAndCalendarioIdAndDataBetweenAndDeletedFalse(
            UUID tenantId, UUID calendarioId, LocalDate inicio, LocalDate fim);

    List<AccCalendarioDia> findByCalendarioIdInAndDataBetweenAndDeletedFalse(
            List<UUID> calendarioIds, LocalDate inicio, LocalDate fim);

    long countByTenantIdAndCalendarioIdAndDeletedFalse(UUID tenantId, UUID calendarioId);
}
