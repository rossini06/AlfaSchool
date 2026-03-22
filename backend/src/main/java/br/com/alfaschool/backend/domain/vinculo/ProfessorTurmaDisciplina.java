package br.com.alfaschool.backend.domain.vinculo;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "professor_turma_disciplina")
public class ProfessorTurmaDisciplina extends BaseEntity {

    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID v) { this.professorId = v; }
    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }
    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }
}
