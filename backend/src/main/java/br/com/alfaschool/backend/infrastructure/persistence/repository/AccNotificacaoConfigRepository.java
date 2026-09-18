package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccNotificacaoConfigRepository extends JpaRepository<AccNotificacaoConfig, UUID> {

    List<AccNotificacaoConfig> findByTenantIdAndDeletedFalseOrderByCanalAsc(UUID tenantId);

    Optional<AccNotificacaoConfig> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccNotificacaoConfig> findFirstByTenantIdAndCanalAndAtivoTrueAndDeletedFalse(UUID tenantId,
                                                                                         CanalNotificacao canal);

    boolean existsByTenantIdAndCanalAndDeletedFalse(UUID tenantId, CanalNotificacao canal);
}
