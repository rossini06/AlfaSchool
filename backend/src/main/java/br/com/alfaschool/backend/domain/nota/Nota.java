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

    @Column(name = "avaliacao_id", nullable = false)
    private UUID avaliacaoId;

    @Column(precision = 5, scale = 2)
    private BigDecimal nota;

    @Column(length = 255)
    private String obs;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getAvaliacaoId() { return avaliacaoId; }
    public void setAvaliacaoId(UUID v) { this.avaliacaoId = v; }
    public BigDecimal getNota() { return nota; }
    public void setNota(BigDecimal v) { this.nota = v; }
    public String getObs() { return obs; }
    public void setObs(String v) { this.obs = v; }
}
