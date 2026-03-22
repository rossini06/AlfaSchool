package br.com.alfaschool.backend.domain.disciplina;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

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

    @Column(name = "curso_id")
    private UUID cursoId;

    @Column(length = 30)
    private String tipo;

    @Column(name = "nota_maxima", precision = 4, scale = 2)
    private BigDecimal notaMaxima;

    @Column(precision = 4, scale = 2)
    private BigDecimal peso;

    @Column(name = "permite_recuperacao")
    private Boolean permiteRecuperacao = true;

    @Column(name = "tipo_avaliacao", length = 30)
    private String tipoAvaliacao;

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
    public UUID getCursoId() { return cursoId; }
    public void setCursoId(UUID v) { this.cursoId = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public BigDecimal getNotaMaxima() { return notaMaxima; }
    public void setNotaMaxima(BigDecimal v) { this.notaMaxima = v; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal v) { this.peso = v; }
    public Boolean getPermiteRecuperacao() { return permiteRecuperacao; }
    public void setPermiteRecuperacao(Boolean v) { this.permiteRecuperacao = v; }
    public String getTipoAvaliacao() { return tipoAvaliacao; }
    public void setTipoAvaliacao(String v) { this.tipoAvaliacao = v; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean v) { this.ativa = v; }
}
