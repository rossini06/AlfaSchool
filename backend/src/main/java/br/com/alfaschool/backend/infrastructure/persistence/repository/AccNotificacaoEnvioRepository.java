package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccNotificacaoEnvioRepository
        extends JpaRepository<AccNotificacaoEnvio, UUID>, JpaSpecificationExecutor<AccNotificacaoEnvio> {

    Optional<AccNotificacaoEnvio> findByTenantIdAndChaveIdempotencia(UUID tenantId, String chaveIdempotencia);

    boolean existsByTenantIdAndChaveIdempotencia(UUID tenantId, String chaveIdempotencia);

    Optional<AccNotificacaoEnvio> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AccNotificacaoEnvio> findByTenantIdAndTitularIdInOrderByCreatedAtDesc(
            UUID tenantId, Collection<UUID> titularIds, Pageable pageable);

    /**
     * CLAIM ATOMICO. Sem este UPDATE condicional dois workers (ou duas
     * instancias do backend) leem a mesma linha PENDENTE e a familia recebe a
     * mensagem duas vezes. Quem conseguir mudar o status leva o trabalho; quem
     * receber 0 linhas afetadas aborta em silencio.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccNotificacaoEnvio e SET e.status = :novoStatus, e.updatedAt = :agora "
            + "WHERE e.id = :id AND e.status = :statusEsperado")
    int reivindicar(@Param("id") UUID id,
                    @Param("statusEsperado") StatusEnvio statusEsperado,
                    @Param("novoStatus") StatusEnvio novoStatus,
                    @Param("agora") Instant agora);

    /**
     * Varredura da fila.
     *
     * <p>{@code agendadoPara IS NOT NULL} nao e' detalhe: e' como o erro
     * permanente sai da fila. FALHOU sem agendamento nunca volta.
     */
    @Query("SELECT e FROM AccNotificacaoEnvio e "
            + "WHERE e.status IN :status "
            + "AND e.agendadoPara IS NOT NULL "
            + "AND e.agendadoPara <= :agora "
            + "ORDER BY e.agendadoPara ASC")
    List<AccNotificacaoEnvio> buscarElegiveis(@Param("status") Collection<StatusEnvio> status,
                                              @Param("agora") Instant agora,
                                              Pageable pageable);

    /** Quantos avisos ja sairam hoje neste canal — alimenta o teto diario. */
    @Query("SELECT COUNT(e) FROM AccNotificacaoEnvio e "
            + "WHERE e.tenantId = :tenantId AND e.canal = :canal "
            + "AND e.status = br.com.alfaschool.backend.domain.access.shared.StatusEnvio.ENVIADO "
            + "AND e.enviadoEm >= :desde")
    long contarEnviadosDesde(@Param("tenantId") UUID tenantId,
                             @Param("canal") CanalNotificacao canal,
                             @Param("desde") Instant desde);

    /**
     * Expiracao em lote. Um aviso de "seu filho entrou na escola" entregue no
     * dia seguinte e' pior do que nao entregue: confunde a familia.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccNotificacaoEnvio e SET e.status = :expirado, "
            + "e.erro = 'EXPIRADO: janela util de 24h vencida', "
            + "e.agendadoPara = NULL, e.updatedAt = :agora "
            + "WHERE e.status IN :status AND e.createdAt < :limite")
    int expirarAntigos(@Param("status") Collection<StatusEnvio> status,
                       @Param("limite") Instant limite,
                       @Param("expirado") StatusEnvio expirado,
                       @Param("agora") Instant agora);
}
