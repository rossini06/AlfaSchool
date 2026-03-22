package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.diario.ConteudoMinistrado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConteudoMinistradoRepository extends JpaRepository<ConteudoMinistrado, UUID> {

    Page<ConteudoMinistrado> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId, Pageable pageable);

    List<ConteudoMinistrado> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalseOrderByDataDesc(
            UUID tenantId, UUID turmaId, UUID disciplinaId);

    Optional<ConteudoMinistrado> findByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId, LocalDate data);

    boolean existsByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId, LocalDate data);

    boolean existsByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalseAndIdNot(
            UUID tenantId, UUID turmaId, UUID disciplinaId, LocalDate data, UUID id);
}
