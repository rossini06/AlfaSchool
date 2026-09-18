package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.equipamento.AccAgentCredencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccAgentCredencialRepository extends JpaRepository<AccAgentCredencial, UUID> {

    /**
     * O login do agente traz o clientId (tenant) explicitamente porque
     * username so' e' unico DENTRO do tenant — procurar so' por username
     * deixaria "agente-portaria" de uma escola casar com o de outra.
     */
    Optional<AccAgentCredencial> findByTenantIdAndUsernameAndDeletedFalse(UUID tenantId, String username);

    Optional<AccAgentCredencial> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<AccAgentCredencial> findByTenantIdAndDeletedFalse(UUID tenantId);

    boolean existsByTenantIdAndUsernameAndDeletedFalse(UUID tenantId, String username);
}
