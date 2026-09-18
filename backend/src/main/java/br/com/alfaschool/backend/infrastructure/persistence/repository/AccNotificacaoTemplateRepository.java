package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoTemplate;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccNotificacaoTemplateRepository extends JpaRepository<AccNotificacaoTemplate, UUID> {

    List<AccNotificacaoTemplate> findByTenantIdAndDeletedFalseOrderByEventoAscCanalAsc(UUID tenantId);

    Optional<AccNotificacaoTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccNotificacaoTemplate> findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(
            UUID tenantId, EventoNotificacao evento, CanalNotificacao canal);
}
