package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.matricula.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, UUID> {
    Page<Matricula> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    List<Matricula> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);
    List<Matricula> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId);
    boolean existsByTenantIdAndAlunoIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID alunoId, UUID turmaId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, String status);
    Optional<Matricula> findByNumeroMatriculaAndTenantIdAndDeletedFalse(String numero, UUID tenantId);
}
