package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    Page<Avaliacao> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Avaliacao> findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID turmaId, UUID disciplinaId);

    List<Avaliacao> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId);
}
