package br.com.alfaschool.backend.domain.diario;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conteudos_ministrados")
public class ConteudoMinistrado extends BaseEntity {

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String objetivos;

    @Column(length = 500)
    private String recursos;

    // Getters e Setters
    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }

    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID v) { this.professorId = v; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { this.data = v; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }

    public String getObjetivos() { return objetivos; }
    public void setObjetivos(String v) { this.objetivos = v; }

    public String getRecursos() { return recursos; }
    public void setRecursos(String v) { this.recursos = v; }
}
