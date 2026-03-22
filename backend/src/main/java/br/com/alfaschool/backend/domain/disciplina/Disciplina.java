package br.com.alfaschool.backend.domain.disciplina;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "disciplinas")
public class Disciplina extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 20)
    private String codigo;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private boolean ativa = true;

    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { this.codigo = v; }
    public Integer getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(Integer v) { this.cargaHoraria = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean v) { this.ativa = v; }
}
