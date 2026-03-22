package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.nota.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    List<Nota> findByAlunoAndAvaliacoes(UUID tenantId, UUID alunoId, List<UUID> avaliacaoIds);
}
