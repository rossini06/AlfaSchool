package br.com.alfaschool.backend.domain.access.evento;

import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
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
 * Livro-razao do modulo: tudo o que o leitor viu, inclusive acesso negado
 * e pessoa desconhecida. Nada e' apagado nem corrigido aqui; interpretacao
 * (presenca, retirada) vive em outras tabelas.
 *
 * titularId fica NULL quando o device_user_id nao casa com nenhuma face.
 * Um dos sistemas anteriores gravava o device_user_id no campo aluno_id e
 * fabricava FK fantasma — aqui DESCONHECIDO + NULL e' a resposta honesta.
 *
 * Nao estende BaseEntity: acc_eventos nao tem created_by/updated_by/
 * deleted de proposito. Evento nao se apaga.
 */
@Entity
@Table(name = "acc_eventos")
public class AccEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "dispositivo_id")
    private UUID dispositivoId;

    @Column(name = "portaria_id")
    private UUID portariaId;

    /** Id do registro dentro do equipamento. Pode repetir apos limpeza de historico. */
    @Column(name = "device_log_id")
    private Long deviceLogId;

    @Column(name = "device_user_id")
    private Long deviceUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "titular_tipo", nullable = false, length = 20)
    private TitularTipo titularTipo = TitularTipo.DESCONHECIDO;

    @Column(name = "titular_id")
    private UUID titularId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoIdentificacao tipo = TipoIdentificacao.FACE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoAcesso resultado = ResultadoAcesso.PERMITIDO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentidoAcesso sentido = SentidoAcesso.INDEFINIDO;

    @Column(length = 255)
    private String motivo;

    /** Momento da leitura, ja convertido do epoch local do firmware. */
    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    /** Momento em que o backend recebeu. Difere de dataHora na fila offline. */
    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemEvento origem = OrigemEvento.AGENTE;

    @Lob
    @Column(name = "raw_json")
    private String rawJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.createdAt = agora;
        this.updatedAt = agora;
        if (this.recebidoEm == null) {
            this.recebidoEm = agora;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public UUID getPortariaId() { return portariaId; }
    public void setPortariaId(UUID v) { this.portariaId = v; }
    public Long getDeviceLogId() { return deviceLogId; }
    public void setDeviceLogId(Long v) { this.deviceLogId = v; }
    public Long getDeviceUserId() { return deviceUserId; }
    public void setDeviceUserId(Long v) { this.deviceUserId = v; }
    public TitularTipo getTitularTipo() { return titularTipo; }
    public void setTitularTipo(TitularTipo v) { this.titularTipo = v; }
    public UUID getTitularId() { return titularId; }
    public void setTitularId(UUID v) { this.titularId = v; }
    public TipoIdentificacao getTipo() { return tipo; }
    public void setTipo(TipoIdentificacao v) { this.tipo = v; }
    public ResultadoAcesso getResultado() { return resultado; }
    public void setResultado(ResultadoAcesso v) { this.resultado = v; }
    public SentidoAcesso getSentido() { return sentido; }
    public void setSentido(SentidoAcesso v) { this.sentido = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) {
        this.motivo = v == null ? null : v.substring(0, Math.min(v.length(), 255));
    }
    public Instant getDataHora() { return dataHora; }
    public void setDataHora(Instant v) { this.dataHora = v; }
    public Instant getRecebidoEm() { return recebidoEm; }
    public void setRecebidoEm(Instant v) { this.recebidoEm = v; }
    public OrigemEvento getOrigem() { return origem; }
    public void setOrigem(OrigemEvento v) { this.origem = v; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String v) { this.rawJson = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
