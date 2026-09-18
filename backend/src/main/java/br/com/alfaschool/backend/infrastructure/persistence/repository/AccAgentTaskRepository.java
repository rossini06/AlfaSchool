package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.equipamento.AccAgentTask;
import br.com.alfaschool.backend.domain.access.equipamento.StatusAgentTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccAgentTaskRepository extends JpaRepository<AccAgentTask, UUID> {

    /**
     * Fila do agente. Tarefa ja vencida nao e' entregue: a consulta filtra
     * expira_em aqui em vez de deixar o agente descobrir depois, senao um
     * "abrir a porta" de meia hora atras chega quando a pessoa ja foi
     * embora — ou, pior, quando outra pessoa esta na frente da catraca.
     */
    @Query("""
            select t from AccAgentTask t
            where t.tenantId = :tenantId
              and t.status = :status
              and (t.expiraEm is null or t.expiraEm > :agora)
              and (:dispositivoId is null or t.dispositivoId = :dispositivoId)
            order by t.createdAt asc
            """)
    List<AccAgentTask> buscarPendentes(@Param("tenantId") UUID tenantId,
                                       @Param("status") StatusAgentTask status,
                                       @Param("dispositivoId") UUID dispositivoId,
                                       @Param("agora") Instant agora,
                                       Pageable pageable);

    Optional<AccAgentTask> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AccAgentTask> findByTenantIdAndStatus(UUID tenantId, StatusAgentTask status);
}
