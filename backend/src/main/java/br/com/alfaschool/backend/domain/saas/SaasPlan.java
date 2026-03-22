package br.com.alfaschool.backend.domain.saas;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "saas_plans")
public class SaasPlan extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 40)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco_mensal", nullable = false)
    private java.math.BigDecimal precoMensal = java.math.BigDecimal.ZERO;

    @Column(name = "preco_anual", nullable = false)
    private java.math.BigDecimal precoAnual = java.math.BigDecimal.ZERO;

    @Column(name = "max_escolas", nullable = false)
    private int maxEscolas = -1;

    @Column(name = "max_usuarios", nullable = false)
    private int maxUsuarios = -1;

    @Column(name = "max_dispositivos", nullable = false)
    private int maxDispositivos = -1;

    @Column(columnDefinition = "JSON")
    private String recursos;

    @Column(nullable = false)
    private boolean ativo = true;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public java.math.BigDecimal getPrecoMensal() { return precoMensal; }
    public void setPrecoMensal(java.math.BigDecimal v) { this.precoMensal = v; }
    public java.math.BigDecimal getPrecoAnual() { return precoAnual; }
    public void setPrecoAnual(java.math.BigDecimal v) { this.precoAnual = v; }
    public int getMaxEscolas() { return maxEscolas; }
    public void setMaxEscolas(int v) { this.maxEscolas = v; }
    public int getMaxUsuarios() { return maxUsuarios; }
    public void setMaxUsuarios(int v) { this.maxUsuarios = v; }
    public int getMaxDispositivos() { return maxDispositivos; }
    public void setMaxDispositivos(int v) { this.maxDispositivos = v; }
    public String getRecursos() { return recursos; }
    public void setRecursos(String v) { this.recursos = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
}
