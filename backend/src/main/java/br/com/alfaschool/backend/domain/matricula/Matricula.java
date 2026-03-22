package br.com.alfaschool.backend.domain.matricula;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "matriculas")
public class Matricula extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "numero_matricula", nullable = false, length = 30)
    private String numeroMatricula;

    @Column(name = "data_matricula", nullable = false)
    private LocalDate dataMatricula;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @Column(nullable = false, length = 20)
    private String status = "ativa";

    @Column(columnDefinition = "TEXT")
    private String obs;

    @Column(length = 30)
    private String tipo = "regular";

    @Column(name = "status_academico", length = 30)
    private String statusAcademico = "cursando";

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal desconto;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID alunoId) { this.alunoId = alunoId; }
    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID turmaId) { this.turmaId = turmaId; }
    public String getNumeroMatricula() { return numeroMatricula; }
    public void setNumeroMatricula(String v) { this.numeroMatricula = v; }
    public LocalDate getDataMatricula() { return dataMatricula; }
    public void setDataMatricula(LocalDate v) { this.dataMatricula = v; }
    public LocalDate getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDate v) { this.dataConclusao = v; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObs() { return obs; }
    public void setObs(String obs) { this.obs = obs; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public String getStatusAcademico() { return statusAcademico; }
    public void setStatusAcademico(String v) { this.statusAcademico = v; }
    public java.math.BigDecimal getDesconto() { return desconto; }
    public void setDesconto(java.math.BigDecimal v) { this.desconto = v; }
}
