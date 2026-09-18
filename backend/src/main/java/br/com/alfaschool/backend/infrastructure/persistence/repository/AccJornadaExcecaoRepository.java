package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.jornada.AccJornadaExcecao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccJornadaExcecaoRepository extends JpaRepository<AccJornadaExcecao, UUID> {

    Optional<AccJornadaExcecao> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccJornadaExcecao> findByTenantIdAndAlunoIdAndDataAndDeletedFalse(UUID tenantId, UUID alunoId, LocalDate data);

    List<AccJornadaExcecao> findByTenantIdAndAlunoIdAndDataBetweenAndDeletedFalse(UUID tenantId, UUID alunoId,
                                                                                 LocalDate inicio, LocalDate fim);

    Page<AccJornadaExcecao> findByTenantIdAndDataBetweenAndDeletedFalse(UUID tenantId, LocalDate inicio,
                                                                       LocalDate fim, Pageable pageable);
}
