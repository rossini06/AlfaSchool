package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
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
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    Page<Avaliacao> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Avaliacao> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId);

    List<Avaliacao> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId);

    Optional<Avaliacao> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    @Query("SELECT a FROM Avaliacao a WHERE a.tenantId = :tenantId AND a.deleted = false " +
           "AND (:turmaId IS NULL OR a.turmaId = :turmaId) " +
           "AND (:disciplinaId IS NULL OR a.disciplinaId = :disciplinaId) " +
           "AND (:periodo IS NULL OR a.periodo = :periodo) " +
           "AND (:status IS NULL OR a.status = :status)")
    Page<Avaliacao> search(@Param("tenantId") UUID tenantId,
                           @Param("turmaId") UUID turmaId,
                           @Param("disciplinaId") UUID disciplinaId,
                           @Param("periodo") String periodo,
                           @Param("status") String status,
                           Pageable pageable);
}
