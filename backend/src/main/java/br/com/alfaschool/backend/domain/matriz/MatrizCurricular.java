package br.com.alfaschool.backend.domain.matriz;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "matriz_curricular")
public class MatrizCurricular extends BaseEntity {

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(nullable = false, length = 60)
    private String periodo;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @Column(nullable = false)
    private boolean obrigatoria = true;

    public UUID getCursoId() { return cursoId; }
    public void setCursoId(UUID v) { this.cursoId = v; }
    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String v) { this.periodo = v; }
    public Integer getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(Integer v) { this.cargaHoraria = v; }
    public boolean isObrigatoria() { return obrigatoria; }
    public void setObrigatoria(boolean v) { this.obrigatoria = v; }
}
