package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.diario.Media;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    // Buscar média por matrícula, disciplina e período
    Optional<Media> findByTenantIdAndMatriculaIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
            UUID tenantId, UUID matriculaId, UUID disciplinaId, String periodo);

    // Buscar todas as médias de uma matrícula
    List<Media> findByTenantIdAndMatriculaIdAndDeletedFalse(UUID tenantId, UUID matriculaId);

    // Buscar médias de uma matrícula por período
    List<Media> findByTenantIdAndMatriculaIdAndPeriodoAndDeletedFalse(UUID tenantId, UUID matriculaId, String periodo);

    // Buscar médias de uma turma e disciplina
    List<Media> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId);

    // Buscar médias por situação
    List<Media> findByTenantIdAndTurmaIdAndSituacaoAndDeletedFalse(
            UUID tenantId, UUID turmaId, SituacaoAluno situacao);

    // Paginação
    Page<Media> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId, Pageable pageable);

    // Verificar existência
    boolean existsByTenantIdAndMatriculaIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
            UUID tenantId, UUID matriculaId, UUID disciplinaId, String periodo);

    // Contagem por situação
    @Query("SELECT COUNT(m) FROM Media m WHERE m.tenantId = :tenantId AND m.turmaId = :turmaId AND m.situacao = :situacao AND m.deleted = false")
    long countByTurmaAndSituacao(@Param("tenantId") UUID tenantId, @Param("turmaId") UUID turmaId, @Param("situacao") SituacaoAluno situacao);
}
