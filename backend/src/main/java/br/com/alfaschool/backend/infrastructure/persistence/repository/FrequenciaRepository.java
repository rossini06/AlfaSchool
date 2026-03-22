package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.diario.StatusFrequencia;
import br.com.alfaschool.backend.domain.frequencia.Frequencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Frequencia> findByTurmaAndDisciplinaAndPeriodo(@Param("tenantId") UUID tenantId, @Param("turmaId") UUID turmaId, @Param("disciplinaId") UUID disciplinaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    Optional<Frequencia> findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
            UUID tenantId, UUID alunoId, UUID turmaId, UUID disciplinaId, LocalDate data);

    // Novo: buscar por matrícula
    List<Frequencia> findByTenantIdAndMatriculaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID matriculaId, UUID disciplinaId);

    // Novo: buscar por matrícula e período
    @Query("SELECT f FROM Frequencia f WHERE f.tenantId = :tenantId AND f.matriculaId = :matriculaId AND f.disciplinaId = :disciplinaId AND f.data BETWEEN :inicio AND :fim AND f.deleted = false")
    List<Frequencia> findByMatriculaAndDisciplinaAndPeriodo(@Param("tenantId") UUID tenantId, @Param("matriculaId") UUID matriculaId, @Param("disciplinaId") UUID disciplinaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Novo: verificar duplicidade com número da aula
    Optional<Frequencia> findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndNumeroAulaAndDeletedFalse(
            UUID tenantId, UUID alunoId, UUID turmaId, UUID disciplinaId, LocalDate data, Integer numeroAula);

    // Novo: contar frequências por status
    @Query("SELECT COUNT(f) FROM Frequencia f WHERE f.tenantId = :tenantId AND f.matriculaId = :matriculaId AND f.disciplinaId = :disciplinaId AND f.status = :status AND f.deleted = false")
    long countByMatriculaAndDisciplinaAndStatus(@Param("tenantId") UUID tenantId, @Param("matriculaId") UUID matriculaId, @Param("disciplinaId") UUID disciplinaId, @Param("status") StatusFrequencia status);

    // Novo: contar total de aulas
    @Query("SELECT COUNT(f) FROM Frequencia f WHERE f.tenantId = :tenantId AND f.matriculaId = :matriculaId AND f.disciplinaId = :disciplinaId AND f.deleted = false")
    long countByMatriculaAndDisciplina(@Param("tenantId") UUID tenantId, @Param("matriculaId") UUID matriculaId, @Param("disciplinaId") UUID disciplinaId);

    // Listagem paginada por turma e disciplina
    Page<Frequencia> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId, Pageable pageable);
}
