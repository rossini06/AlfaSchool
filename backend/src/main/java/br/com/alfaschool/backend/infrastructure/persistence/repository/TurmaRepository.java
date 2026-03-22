package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.turma.Turma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TurmaRepository extends JpaRepository<Turma, UUID> {
    Page<Turma> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    List<Turma> findByTenantIdAndCursoIdAndDeletedFalse(UUID tenantId, UUID cursoId);
    Page<Turma> findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(UUID tenantId, String nome, Pageable pageable);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
