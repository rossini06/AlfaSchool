package br.com.alfaschool.backend.domain.turma;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "turmas")
public class Turma extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(length = 20)
    private String codigo;

    @Column(name = "ano_letivo", nullable = false)
    private int anoLetivo;

    @Column(nullable = false, length = 20)
    private String turno = "manha";

    @Column(name = "professor_responsavel", length = 120)
    private String professorResponsavel;

    @Column(name = "capacidade_maxima", nullable = false)
    private int capacidadeMaxima = 40;

    @Column(nullable = false)
    private boolean ativa = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getCursoId() { return cursoId; }
    public void setCursoId(UUID cursoId) { this.cursoId = cursoId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public int getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(int anoLetivo) { this.anoLetivo = anoLetivo; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public String getProfessorResponsavel() { return professorResponsavel; }
    public void setProfessorResponsavel(String v) { this.professorResponsavel = v; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public void setCapacidadeMaxima(int capacidadeMaxima) { this.capacidadeMaxima = capacidadeMaxima; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
