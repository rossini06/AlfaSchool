package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.nota.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotaRepository extends JpaRepository<Nota, UUID> {

    List<Nota> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    List<Nota> findByTenantIdAndAvaliacaoIdAndDeletedFalse(UUID tenantId, UUID avaliacaoId);

    Optional<Nota> findByTenantIdAndAlunoIdAndAvaliacaoIdAndDeletedFalse(
            UUID tenantId, UUID alunoId, UUID avaliacaoId);

    @Query("SELECT n FROM Nota n WHERE n.tenantId = :tenantId AND n.alunoId = :alunoId AND n.avaliacaoId IN :avaliacaoIds AND n.deleted = false")
    List<Nota> findByAlunoAndAvaliacoes(@Param("tenantId") UUID tenantId, @Param("alunoId") UUID alunoId, @Param("avaliacaoIds") List<UUID> avaliacaoIds);

    // Novo: buscar por matrícula
    List<Nota> findByTenantIdAndMatriculaIdAndDeletedFalse(UUID tenantId, UUID matriculaId);

    // Novo: buscar nota por matrícula e avaliação
    Optional<Nota> findByTenantIdAndMatriculaIdAndAvaliacaoIdAndDeletedFalse(
            UUID tenantId, UUID matriculaId, UUID avaliacaoId);

    // Novo: buscar notas por matrícula e lista de avaliações
    @Query("SELECT n FROM Nota n WHERE n.tenantId = :tenantId AND n.matriculaId = :matriculaId AND n.avaliacaoId IN :avaliacaoIds AND n.deleted = false")
    List<Nota> findByMatriculaAndAvaliacoes(@Param("tenantId") UUID tenantId, @Param("matriculaId") UUID matriculaId, @Param("avaliacaoIds") List<UUID> avaliacaoIds);

    // Novo: buscar notas por matrícula e disciplina (para cálculo de média)
    @Query("SELECT n FROM Nota n WHERE n.tenantId = :tenantId AND n.matriculaId = :matriculaId " +
           "AND n.avaliacaoId IN (SELECT a.id FROM Avaliacao a WHERE a.disciplinaId = :disciplinaId AND a.deleted = false) " +
           "AND n.deleted = false")
    List<Nota> findByMatriculaAndDisciplina(@Param("tenantId") UUID tenantId, @Param("matriculaId") UUID matriculaId, @Param("disciplinaId") UUID disciplinaId);
}
