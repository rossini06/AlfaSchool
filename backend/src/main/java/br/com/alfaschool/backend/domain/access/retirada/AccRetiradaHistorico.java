package br.com.alfaschool.backend.domain.access.retirada;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
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
 * Trilha append-only das transicoes de uma retirada.
 *
 * Nao estende BaseEntity de proposito: a tabela nao tem updated_at nem
 * deleted. Linha de auditoria nao se edita nem se apaga — se fosse
 * editavel, deixaria de servir a quem precisa provar quem entregou a
 * crianca.
 */
@Entity
@Table(name = "acc_retirada_historico")
public class AccRetiradaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "retirada_id", nullable = false, updatable = false)
    private UUID retiradaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusRetirada statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusRetirada statusNovo;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemTransicao origem = OrigemTransicao.PAINEL;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getRetiradaId() { return retiradaId; }
    public void setRetiradaId(UUID v) { this.retiradaId = v; }
    public StatusRetirada getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusRetirada v) { this.statusAnterior = v; }
    public StatusRetirada getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusRetirada v) { this.statusNovo = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public OrigemTransicao getOrigem() { return origem; }
    public void setOrigem(OrigemTransicao v) { this.origem = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { this.motivo = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
