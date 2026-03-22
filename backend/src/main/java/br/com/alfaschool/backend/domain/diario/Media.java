package br.com.alfaschool.backend.domain.diario;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "medias")
public class Media extends BaseEntity {

    @Column(name = "matricula_id", nullable = false)
    private UUID matriculaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(length = 20)
    private String periodo;

    @Column(precision = 5, scale = 2)
    private BigDecimal media;

    @Column(name = "percentual_frequencia", precision = 5, scale = 2)
    private BigDecimal percentualFrequencia;

    @Column(name = "total_aulas")
    private Integer totalAulas = 0;

    @Column(name = "total_presencas")
    private Integer totalPresencas = 0;

    @Column(name = "total_faltas")
    private Integer totalFaltas = 0;

    @Column(name = "total_justificadas")
    private Integer totalJustificadas = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SituacaoAluno situacao = SituacaoAluno.CURSANDO;

    @Column(length = 2)
    private String conceito;

    @Column(name = "observacao_descritiva", columnDefinition = "TEXT")
    private String observacaoDescritiva;

    // Getters e Setters
    public UUID getMatriculaId() { return matriculaId; }
    public void setMatriculaId(UUID v) { this.matriculaId = v; }

    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }

    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String v) { this.periodo = v; }

    public BigDecimal getMedia() { return media; }
    public void setMedia(BigDecimal v) { this.media = v; }

    public BigDecimal getPercentualFrequencia() { return percentualFrequencia; }
    public void setPercentualFrequencia(BigDecimal v) { this.percentualFrequencia = v; }

    public Integer getTotalAulas() { return totalAulas; }
    public void setTotalAulas(Integer v) { this.totalAulas = v; }

    public Integer getTotalPresencas() { return totalPresencas; }
    public void setTotalPresencas(Integer v) { this.totalPresencas = v; }

    public Integer getTotalFaltas() { return totalFaltas; }
    public void setTotalFaltas(Integer v) { this.totalFaltas = v; }

    public Integer getTotalJustificadas() { return totalJustificadas; }
    public void setTotalJustificadas(Integer v) { this.totalJustificadas = v; }

    public SituacaoAluno getSituacao() { return situacao; }
    public void setSituacao(SituacaoAluno v) { this.situacao = v; }

    public String getConceito() { return conceito; }
    public void setConceito(String v) { this.conceito = v; }

    public String getObservacaoDescritiva() { return observacaoDescritiva; }
    public void setObservacaoDescritiva(String v) { this.observacaoDescritiva = v; }
}
