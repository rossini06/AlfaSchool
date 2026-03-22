package br.com.alfaschool.backend.domain.frequencia;

import br.com.alfaschool.backend.domain.diario.StatusFrequencia;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "frequencias")
public class Frequencia extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "matricula_id")
    private UUID matriculaId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "numero_aula")
    private Integer numeroAula = 1;

    @Column(nullable = false)
    private boolean presente = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusFrequencia status = StatusFrequencia.PRESENTE;

    @Column(length = 255)
    private String obs;

    // Getters e Setters
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }

    public UUID getMatriculaId() { return matriculaId; }
    public void setMatriculaId(UUID v) { this.matriculaId = v; }

    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }

    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { this.data = v; }

    public Integer getNumeroAula() { return numeroAula; }
    public void setNumeroAula(Integer v) { this.numeroAula = v; }

    public boolean isPresente() { return presente; }
    public void setPresente(boolean v) { this.presente = v; }

    public StatusFrequencia getStatus() { return status; }
    public void setStatus(StatusFrequencia v) {
        this.status = v;
        this.presente = (v == StatusFrequencia.PRESENTE || v == StatusFrequencia.ATRASADO);
    }

    public String getObs() { return obs; }
    public void setObs(String v) { this.obs = v; }
}
