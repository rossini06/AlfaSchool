package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.curso.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CursoRepository extends JpaRepository<Curso, UUID> {
    Page<Curso> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<Curso> findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(UUID tenantId, String nome, Pageable pageable);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
    boolean existsByTenantIdAndNomeIgnoreCaseAndDeletedFalse(UUID tenantId, String nome);
    boolean existsByTenantIdAndNomeIgnoreCaseAndDeletedFalseAndIdNot(UUID tenantId, String nome, UUID id);
}
