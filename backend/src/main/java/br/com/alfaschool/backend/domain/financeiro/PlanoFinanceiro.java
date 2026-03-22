package br.com.alfaschool.backend.domain.financeiro;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "planos_financeiros")
public class PlanoFinanceiro extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 20, nullable = false)
    private String periodicidade = "mensal";

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal v) { this.valor = v; }
    public String getPeriodicidade() { return periodicidade; }
    public void setPeriodicidade(String v) { this.periodicidade = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
}
