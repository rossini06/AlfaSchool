package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.frequencia.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FrequenciaRepository extends JpaRepository<Frequencia, UUID> {

    List<Frequencia> findByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId, LocalDate data);

    List<Frequencia> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    @Query("SELECT f FROM Frequencia f WHERE f.tenantId = :tenantId AND f.turmaId = :turmaId AND f.disciplinaId = :disciplinaId AND f.data BETWEEN :inicio AND :fim AND f.deleted = false")
    List<Frequencia> findByTurmaAndDisciplinaAndPeriodo(UUID tenantId, UUID turmaId, UUID disciplinaId, LocalDate inicio, LocalDate fim);

    Optional<Frequencia> findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
            UUID tenantId, UUID alunoId, UUID turmaId, UUID disciplinaId, LocalDate data);
}
