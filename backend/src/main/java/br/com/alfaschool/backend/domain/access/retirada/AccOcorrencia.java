package br.com.alfaschool.backend.domain.access.retirada;

import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Situacao que exige intervencao humana: tentativa nao autorizada, pessoa
 * desconhecida, retirada manual, equipamento offline.
 *
 * E' o registro que sobra quando o fluxo automatico nao deu conta. Por isso
 * ela nunca e' gerada em silencio: quem registra tambem notifica a
 * coordenacao.
 */
@Entity
@Table(name = "acc_ocorrencias")
public class AccOcorrencia extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoOcorrencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravidade", nullable = false, length = 20)
    private GravidadeOcorrencia gravidade = GravidadeOcorrencia.MEDIA;

    @Column(name = "aluno_id")
    private UUID alunoId;

    @Column(name = "pessoa_autorizada_id")
    private UUID pessoaAutorizadaId;

    @Column(name = "dispositivo_id")
    private UUID dispositivoId;

    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "retirada_id")
    private UUID retiradaId;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusOcorrencia status = StatusOcorrencia.ABERTA;

    /**
     * Quando o fato aconteceu. Diferente de createdAt, que e' quando
     * alguem registrou — a coordenacao lanca as 17h o episodio das 14h.
     */
    @Column(name = "ocorrido_em")
    private Instant ocorridoEm;

    @Column(name = "tratado_por_user_id")
    private UUID tratadoPorUserId;

    @Column(name = "tratado_em")
    private Instant tratadoEm;

    @Column(name = "tratativa", columnDefinition = "TEXT")
    private String tratativa;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public TipoOcorrencia getTipo() { return tipo; }
    public void setTipo(TipoOcorrencia v) { this.tipo = v; }
    public GravidadeOcorrencia getGravidade() { return gravidade; }
    public void setGravidade(GravidadeOcorrencia v) { this.gravidade = v; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getPessoaAutorizadaId() { return pessoaAutorizadaId; }
    public void setPessoaAutorizadaId(UUID v) { this.pessoaAutorizadaId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public UUID getEventoId() { return eventoId; }
    public void setEventoId(UUID v) { this.eventoId = v; }
    public UUID getRetiradaId() { return retiradaId; }
    public void setRetiradaId(UUID v) { this.retiradaId = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(Instant ocorridoEm) { this.ocorridoEm = ocorridoEm; }

    public StatusOcorrencia getStatus() { return status; }
    public void setStatus(StatusOcorrencia v) { this.status = v; }
    public UUID getTratadoPorUserId() { return tratadoPorUserId; }
    public void setTratadoPorUserId(UUID v) { this.tratadoPorUserId = v; }
    public Instant getTratadoEm() { return tratadoEm; }
    public void setTratadoEm(Instant v) { this.tratadoEm = v; }
    public String getTratativa() { return tratativa; }
    public void setTratativa(String v) { this.tratativa = v; }
}
