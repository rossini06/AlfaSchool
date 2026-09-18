package br.com.alfaschool.backend.domain.access.autorizacao;

import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trilha APPEND-ONLY de tudo que aconteceu com uma autorizacao.
 *
 * Nunca atualize nem apague uma linha daqui: e' a unica prova de quem
 * liberou, suspendeu ou revogou a retirada de uma crianca, e e' o que a
 * escola apresenta quando um responsavel contesta. Por isso a entidade
 * NAO estende BaseEntity (nao ha updatedAt nem deleted na tabela) e NAO
 * tem setters — so da para construir e ler.
 *
 * Corrigir um registro errado significa gravar uma NOVA linha explicando,
 * jamais reescrever a anterior.
 */
@Entity
@Table(name = "acc_autorizacao_historico")
public class AutorizacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "autorizacao_id", nullable = false, updatable = false)
    private UUID autorizacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AcaoAutorizacao acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20, updatable = false)
    private StatusAutorizacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 20, updatable = false)
    private StatusAutorizacao statusNovo;

    /** Nulo apenas quando a acao foi do sistema (job de expiracao). */
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(length = 255, updatable = false)
    private String motivo;

    @Column(length = 45, updatable = false)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AutorizacaoHistorico() {
        // exigido pelo JPA
    }

    public AutorizacaoHistorico(UUID tenantId,
                                UUID autorizacaoId,
                                AcaoAutorizacao acao,
                                StatusAutorizacao statusAnterior,
                                StatusAutorizacao statusNovo,
                                UUID userId,
                                String motivo,
                                String ip) {
        this.tenantId = tenantId;
        this.autorizacaoId = autorizacaoId;
        this.acao = acao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.userId = userId;
        this.motivo = motivo;
        this.ip = ip;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAutorizacaoId() { return autorizacaoId; }
    public AcaoAutorizacao getAcao() { return acao; }
    public StatusAutorizacao getStatusAnterior() { return statusAnterior; }
    public StatusAutorizacao getStatusNovo() { return statusNovo; }
    public UUID getUserId() { return userId; }
    public String getMotivo() { return motivo; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
}
