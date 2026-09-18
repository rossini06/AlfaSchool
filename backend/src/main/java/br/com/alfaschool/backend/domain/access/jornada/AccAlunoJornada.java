package br.com.alfaschool.backend.domain.access.jornada;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vinculo aluno &lt;-&gt; jornada com vigencia.
 *
 * A vigencia existe para que trocar o plano no meio do ano NAO reescreva o
 * passado: a apuracao de marco continua resolvendo a jornada que valia em
 * marco, mesmo depois que a familia migrou para o plano de 5h em agosto.
 *
 * O vinculo e' com o ALUNO, nunca com a turma: a mesma turma tem o Pedro
 * que sai ao meio-dia e a Ana que fica ate as 17h.
 */
@Entity
@Table(name = "acc_aluno_jornadas")
public class AccAlunoJornada extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "jornada_id", nullable = false)
    private UUID jornadaId;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    /** Nulo = vigente por prazo indeterminado. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(length = 255)
    private String observacao;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getJornadaId() { return jornadaId; }
    public void setJornadaId(UUID v) { this.jornadaId = v; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public void setVigenciaInicio(LocalDate v) { this.vigenciaInicio = v; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
    public void setVigenciaFim(LocalDate v) { this.vigenciaFim = v; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String v) { this.observacao = v; }

    /** Vigente na data quando inicio &lt;= data e (fim nulo ou fim &gt;= data). */
    public boolean vigenteEm(LocalDate data) {
        if (vigenciaInicio == null || data.isBefore(vigenciaInicio)) {
            return false;
        }
        return vigenciaFim == null || !data.isAfter(vigenciaFim);
    }
}
