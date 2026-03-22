package br.com.alfaschool.backend.domain.frequencia;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "frequencias")
public class Frequencia extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean presente = true;

    @Column(length = 255)
    private String obs;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }
    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { this.data = v; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean v) { this.presente = v; }
    public String getObs() { return obs; }
    public void setObs(String v) { this.obs = v; }
}
