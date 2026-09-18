package br.com.alfaschool.backend.domain.access.retirada;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Um pedido de retirada de aluno.
 *
 * Os tres carimbos de tempo nunca sao o mesmo momento e nunca podem ser
 * colapsados:
 *   solicitadoEm - o responsavel foi reconhecido na portaria
 *   entregueEm   - um colaborador confirmou a entrega da crianca
 *   saidaEm      - a saida efetiva foi registrada (so' isso fecha a permanencia)
 *
 * O pai que chega 17h e recebe o filho 17h20 gera permanencia ate 17h20.
 * A diferenca entre solicitadoEm e entregueEm e' o tempo de espera que a
 * coordenacao cobra no relatorio.
 */
@Entity
@Table(name = "acc_retiradas")
public class AccRetirada extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "pessoa_autorizada_id")
    private UUID pessoaAutorizadaId;

    @Column(name = "autorizacao_id")
    private UUID autorizacaoId;

    @Column(name = "portaria_id")
    private UUID portariaId;

    @Column(name = "dispositivo_id")
    private UUID dispositivoId;

    @Column(name = "turma_id")
    private UUID turmaId;

    @Column(name = "sala_id")
    private UUID salaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusRetirada status = StatusRetirada.SOLICITADA;

    @Column(name = "ordem_chegada")
    private Integer ordemChegada;

    @Column(name = "solicitado_em")
    private Instant solicitadoEm;

    @Column(name = "solicitacao_evento_id")
    private UUID solicitacaoEventoId;

    @Column(name = "preparando_em")
    private Instant preparandoEm;

    @Column(name = "preparado_por_user_id")
    private UUID preparadoPorUserId;

    @Column(name = "pronto_em")
    private Instant prontoEm;

    @Column(name = "entregue_em")
    private Instant entregueEm;

    /** Ato de responsabilidade: nunca nulo depois de ENTREGUE. */
    @Column(name = "entregue_por_user_id")
    private UUID entreguePorUserId;

    @Column(name = "saida_em")
    private Instant saidaEm;

    @Column(name = "saida_evento_id")
    private UUID saidaEventoId;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    @Column(name = "cancelado_por_user_id")
    private UUID canceladoPorUserId;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "retirada_manual", nullable = false)
    private boolean retiradaManual = false;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    /**
     * Tempo de espera do responsavel: da chegada ate a entrega. Enquanto a
     * crianca nao foi entregue o relogio continua correndo, por isso o
     * calculo usa "agora" como fim em aberto.
     */
    public long tempoEsperaMinutos(Instant agora) {
        if (solicitadoEm == null) {
            return 0L;
        }
        Instant fim = entregueEm != null ? entregueEm : agora;
        if (fim.isBefore(solicitadoEm)) {
            return 0L;
        }
        return Duration.between(solicitadoEm, fim).toMinutes();
    }

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getPessoaAutorizadaId() { return pessoaAutorizadaId; }
    public void setPessoaAutorizadaId(UUID v) { this.pessoaAutorizadaId = v; }
    public UUID getAutorizacaoId() { return autorizacaoId; }
    public void setAutorizacaoId(UUID v) { this.autorizacaoId = v; }
    public UUID getPortariaId() { return portariaId; }
    public void setPortariaId(UUID v) { this.portariaId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }
    public UUID getSalaId() { return salaId; }
    public void setSalaId(UUID v) { this.salaId = v; }
    public StatusRetirada getStatus() { return status; }
    public void setStatus(StatusRetirada v) { this.status = v; }
    public Integer getOrdemChegada() { return ordemChegada; }
    public void setOrdemChegada(Integer v) { this.ordemChegada = v; }
    public Instant getSolicitadoEm() { return solicitadoEm; }
    public void setSolicitadoEm(Instant v) { this.solicitadoEm = v; }
    public UUID getSolicitacaoEventoId() { return solicitacaoEventoId; }
    public void setSolicitacaoEventoId(UUID v) { this.solicitacaoEventoId = v; }
    public Instant getPreparandoEm() { return preparandoEm; }
    public void setPreparandoEm(Instant v) { this.preparandoEm = v; }
    public UUID getPreparadoPorUserId() { return preparadoPorUserId; }
    public void setPreparadoPorUserId(UUID v) { this.preparadoPorUserId = v; }
    public Instant getProntoEm() { return prontoEm; }
    public void setProntoEm(Instant v) { this.prontoEm = v; }
    public Instant getEntregueEm() { return entregueEm; }
    public void setEntregueEm(Instant v) { this.entregueEm = v; }
    public UUID getEntreguePorUserId() { return entreguePorUserId; }
    public void setEntreguePorUserId(UUID v) { this.entreguePorUserId = v; }
    public Instant getSaidaEm() { return saidaEm; }
    public void setSaidaEm(Instant v) { this.saidaEm = v; }
    public UUID getSaidaEventoId() { return saidaEventoId; }
    public void setSaidaEventoId(UUID v) { this.saidaEventoId = v; }
    public Instant getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(Instant v) { this.canceladoEm = v; }
    public UUID getCanceladoPorUserId() { return canceladoPorUserId; }
    public void setCanceladoPorUserId(UUID v) { this.canceladoPorUserId = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { this.motivo = v; }
    public boolean isRetiradaManual() { return retiradaManual; }
    public void setRetiradaManual(boolean v) { this.retiradaManual = v; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String v) { this.observacao = v; }
}
