package br.com.alfaschool.backend.domain.nota;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "notas")
public class Nota extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "matricula_id")
    private UUID matriculaId;

    @Column(name = "avaliacao_id", nullable = false)
    private UUID avaliacaoId;

    @Column(precision = 5, scale = 2)
    private BigDecimal nota;

    @Column(name = "nota_recuperacao", precision = 5, scale = 2)
    private BigDecimal notaRecuperacao;

    @Column(name = "nota_final", precision = 5, scale = 2)
    private BigDecimal notaFinal;

    @Column(length = 255)
    private String obs;

    // Getters e Setters
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }

    public UUID getMatriculaId() { return matriculaId; }
    public void setMatriculaId(UUID v) { this.matriculaId = v; }

    public UUID getAvaliacaoId() { return avaliacaoId; }
    public void setAvaliacaoId(UUID v) { this.avaliacaoId = v; }

    public BigDecimal getNota() { return nota; }
    public void setNota(BigDecimal v) { this.nota = v; }

    public BigDecimal getNotaRecuperacao() { return notaRecuperacao; }
    public void setNotaRecuperacao(BigDecimal v) { this.notaRecuperacao = v; }

    public BigDecimal getNotaFinal() { return notaFinal; }
    public void setNotaFinal(BigDecimal v) { this.notaFinal = v; }

    public String getObs() { return obs; }
    public void setObs(String v) { this.obs = v; }

    /**
     * Calcula a nota final como o maior valor entre nota e nota de recuperação
     */
    public void calcularNotaFinal() {
        if (nota == null && notaRecuperacao == null) {
            this.notaFinal = null;
        } else if (nota == null) {
            this.notaFinal = notaRecuperacao;
        } else if (notaRecuperacao == null) {
            this.notaFinal = nota;
        } else {
            this.notaFinal = nota.compareTo(notaRecuperacao) >= 0 ? nota : notaRecuperacao;
        }
    }
}
