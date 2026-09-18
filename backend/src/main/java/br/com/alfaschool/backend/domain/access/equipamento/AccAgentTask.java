package br.com.alfaschool.backend.domain.access.equipamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de comandos servidor -> agente. O backend nunca alcanca a LAN da
 * escola: e' o agente que faz polling e devolve o resultado.
 *
 * Nao estende BaseEntity: acc_agent_tasks nao tem created_by/updated_by/
 * deleted. Tarefa concluida vira historico, nao e' apagada.
 */
@Entity
@Table(name = "acc_agent_tasks")
public class AccAgentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "dispositivo_id")
    private UUID dispositivoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAgentTask tipo;

    @Lob
    @Column(name = "parametros")
    private String parametros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgentTask status = StatusAgentTask.PENDENTE;

    @Lob
    @Column(name = "resultado")
    private String resultado;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.updatedAt = Instant.now();
    }

    /** Tarefa vencida nao deve ser entregue ao agente: abrir porta atrasado e' pior que nao abrir. */
    public boolean expirada(Instant referencia) {
        return expiraEm != null && referencia.isAfter(expiraEm);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public TipoAgentTask getTipo() { return tipo; }
    public void setTipo(TipoAgentTask v) { this.tipo = v; }
    public String getParametros() { return parametros; }
    public void setParametros(String v) { this.parametros = v; }
    public StatusAgentTask getStatus() { return status; }
    public void setStatus(StatusAgentTask v) { this.status = v; }
    public String getResultado() { return resultado; }
    public void setResultado(String v) { this.resultado = v; }
    public int getTentativas() { return tentativas; }
    public void setTentativas(int v) { this.tentativas = v; }
    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant v) { this.expiraEm = v; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(Instant v) { this.concluidoEm = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
