package br.com.alfaschool.backend.domain.access.autorizacao;

import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Liga um aluno a uma pessoa autorizada. E' aqui que mora a janela de
 * validade: sem vigencia, dias e horario na propria autorizacao, uma
 * autorizacao de "nesta sexta a tia busca" viraria permanente por descuido.
 *
 * Invariantes garantidas pelo service (ver AutorizacaoRetiradaService):
 * - permanente=false EXIGE vigenciaFim;
 * - origem PORTAL nasce PENDENTE e so vale depois de aprovada pela escola;
 * - toda mudanca de status grava AutorizacaoHistorico.
 */
@Entity
@Table(name = "acc_autorizacoes_retirada")
public class AutorizacaoRetirada extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "pessoa_autorizada_id", nullable = false)
    private UUID pessoaAutorizadaId;

    @Column(nullable = false)
    private boolean permanente = true;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    /** Obrigatoria quando permanente=false. E' o que impede a temporaria eterna. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    /**
     * CSV de dias da semana no padrao ISO-8601, igual ao DayOfWeek do Java:
     * 1=segunda ... 7=domingo. Ex.: "1,3,5". Vazio/nulo = todos os dias.
     * Usamos ISO para nao existir conversao entre o que e' gravado e o que
     * LocalDate.getDayOfWeek().getValue() devolve — conversao aqui liberaria
     * a pessoa no dia errado.
     */
    @Column(name = "dias_semana", length = 20)
    private String diasSemana;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAutorizacao status = StatusAutorizacao.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemAutorizacao origem = OrigemAutorizacao.ESCOLA;

    @Column(length = 255)
    private String motivo;

    /** Documento em storage restrito (procuracao etc.). Nao exponha em listagem comum. */
    @Column(name = "documento_key", length = 255)
    private String documentoKey;

    @Column(name = "aprovado_por_user_id")
    private UUID aprovadoPorUserId;

    @Column(name = "aprovado_em")
    private Instant aprovadoEm;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID alunoId) { this.alunoId = alunoId; }
    public UUID getPessoaAutorizadaId() { return pessoaAutorizadaId; }
    public void setPessoaAutorizadaId(UUID pessoaAutorizadaId) { this.pessoaAutorizadaId = pessoaAutorizadaId; }
    public boolean isPermanente() { return permanente; }
    public void setPermanente(boolean permanente) { this.permanente = permanente; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public void setVigenciaInicio(LocalDate vigenciaInicio) { this.vigenciaInicio = vigenciaInicio; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
    public void setVigenciaFim(LocalDate vigenciaFim) { this.vigenciaFim = vigenciaFim; }
    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public StatusAutorizacao getStatus() { return status; }
    public void setStatus(StatusAutorizacao status) { this.status = status; }
    public OrigemAutorizacao getOrigem() { return origem; }
    public void setOrigem(OrigemAutorizacao origem) { this.origem = origem; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDocumentoKey() { return documentoKey; }
    public void setDocumentoKey(String documentoKey) { this.documentoKey = documentoKey; }
    public UUID getAprovadoPorUserId() { return aprovadoPorUserId; }
    public void setAprovadoPorUserId(UUID aprovadoPorUserId) { this.aprovadoPorUserId = aprovadoPorUserId; }
    public Instant getAprovadoEm() { return aprovadoEm; }
    public void setAprovadoEm(Instant aprovadoEm) { this.aprovadoEm = aprovadoEm; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
