package br.com.alfaschool.backend.domain.curso;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cursos")
public class Curso extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 20)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @Column(length = 20, nullable = false)
    private String modalidade = "presencial";

    @Column(length = 30)
    private String nivel;

    @Column(length = 30)
    private String tipo;

    @Column(name = "duracao_meses")
    private Integer duracaoMeses;

    @Column(name = "idade_minima")
    private Integer idadeMinima;

    @Column(name = "idade_maxima")
    private Integer idadeMaxima;

    @Column(name = "preco_base", precision = 10, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "nota_minima_aprovacao", precision = 4, scale = 2)
    private BigDecimal notaMinimaAprovacao;

    @Column(name = "frequencia_minima_aprovacao", precision = 5, scale = 2)
    private BigDecimal frequenciaMinimaAprovacao;

    @Column(nullable = false)
    private boolean ativo = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(Integer cargaHoraria) { this.cargaHoraria = cargaHoraria; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getDuracaoMeses() { return duracaoMeses; }
    public void setDuracaoMeses(Integer duracaoMeses) { this.duracaoMeses = duracaoMeses; }
    public Integer getIdadeMinima() { return idadeMinima; }
    public void setIdadeMinima(Integer idadeMinima) { this.idadeMinima = idadeMinima; }
    public Integer getIdadeMaxima() { return idadeMaxima; }
    public void setIdadeMaxima(Integer idadeMaxima) { this.idadeMaxima = idadeMaxima; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public void setPrecoBase(BigDecimal precoBase) { this.precoBase = precoBase; }
    public BigDecimal getNotaMinimaAprovacao() { return notaMinimaAprovacao; }
    public void setNotaMinimaAprovacao(BigDecimal notaMinimaAprovacao) { this.notaMinimaAprovacao = notaMinimaAprovacao; }
    public BigDecimal getFrequenciaMinimaAprovacao() { return frequenciaMinimaAprovacao; }
    public void setFrequenciaMinimaAprovacao(BigDecimal frequenciaMinimaAprovacao) { this.frequenciaMinimaAprovacao = frequenciaMinimaAprovacao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
