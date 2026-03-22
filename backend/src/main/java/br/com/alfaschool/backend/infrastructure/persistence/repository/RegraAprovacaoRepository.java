package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.TipoEnsino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegraAprovacaoRepository extends JpaRepository<RegraAprovacao, UUID> {

    Optional<RegraAprovacao> findByTenantIdAndTipoEnsinoAndDeletedFalse(UUID tenantId, TipoEnsino tipoEnsino);

    List<RegraAprovacao> findByTenantIdAndDeletedFalse(UUID tenantId);

    boolean existsByTenantIdAndTipoEnsinoAndDeletedFalse(UUID tenantId, TipoEnsino tipoEnsino);

    boolean existsByTenantIdAndTipoEnsinoAndDeletedFalseAndIdNot(UUID tenantId, TipoEnsino tipoEnsino, UUID id);
}
