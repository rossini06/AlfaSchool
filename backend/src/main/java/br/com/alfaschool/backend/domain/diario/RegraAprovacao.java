package br.com.alfaschool.backend.domain.diario;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "regras_aprovacao")
public class RegraAprovacao extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ensino", nullable = false, length = 30)
    private TipoEnsino tipoEnsino;

    @Column(name = "usa_nota_numerica")
    private Boolean usaNotaNumerica = true;

    @Column(name = "usa_conceito")
    private Boolean usaConceito = false;

    @Column(name = "usa_avaliacao_descritiva")
    private Boolean usaAvaliacaoDescritiva = false;

    @Column(name = "nota_minima_aprovacao", precision = 4, scale = 2)
    private BigDecimal notaMinimaAprovacao = new BigDecimal("6.00");

    @Column(name = "frequencia_minima_aprovacao", precision = 5, scale = 2)
    private BigDecimal frequenciaMinimaAprovacao = new BigDecimal("75.00");

    @Column(name = "permite_recuperacao")
    private Boolean permiteRecuperacao = true;

    @Column(name = "calcula_media_aritmetica")
    private Boolean calculaMediaAritmetica = true;

    @Column(name = "calcula_media_ponderada")
    private Boolean calculaMediaPonderada = false;

    @Column(name = "exige_projeto_final")
    private Boolean exigeProjetoFinal = false;

    @Column(name = "aprovacao_por_disciplina")
    private Boolean aprovacaoPorDisciplina = false;

    @Column(name = "aprovacao_por_modulo")
    private Boolean aprovacaoPorModulo = false;

    @Column(name = "conceitos_possiveis", length = 50)
    private String conceitosPossiveis = "A,B,C,D,E";

    @Column(name = "conceito_minimo_aprovacao", length = 2)
    private String conceitoMinimoAprovacao = "C";

    // Getters e Setters
    public TipoEnsino getTipoEnsino() { return tipoEnsino; }
    public void setTipoEnsino(TipoEnsino v) { this.tipoEnsino = v; }

    public Boolean getUsaNotaNumerica() { return usaNotaNumerica; }
    public void setUsaNotaNumerica(Boolean v) { this.usaNotaNumerica = v; }

    public Boolean getUsaConceito() { return usaConceito; }
    public void setUsaConceito(Boolean v) { this.usaConceito = v; }

    public Boolean getUsaAvaliacaoDescritiva() { return usaAvaliacaoDescritiva; }
    public void setUsaAvaliacaoDescritiva(Boolean v) { this.usaAvaliacaoDescritiva = v; }

    public BigDecimal getNotaMinimaAprovacao() { return notaMinimaAprovacao; }
    public void setNotaMinimaAprovacao(BigDecimal v) { this.notaMinimaAprovacao = v; }

    public BigDecimal getFrequenciaMinimaAprovacao() { return frequenciaMinimaAprovacao; }
    public void setFrequenciaMinimaAprovacao(BigDecimal v) { this.frequenciaMinimaAprovacao = v; }

    public Boolean getPermiteRecuperacao() { return permiteRecuperacao; }
    public void setPermiteRecuperacao(Boolean v) { this.permiteRecuperacao = v; }

    public Boolean getCalculaMediaAritmetica() { return calculaMediaAritmetica; }
    public void setCalculaMediaAritmetica(Boolean v) { this.calculaMediaAritmetica = v; }

    public Boolean getCalculaMediaPonderada() { return calculaMediaPonderada; }
    public void setCalculaMediaPonderada(Boolean v) { this.calculaMediaPonderada = v; }

    public Boolean getExigeProjetoFinal() { return exigeProjetoFinal; }
    public void setExigeProjetoFinal(Boolean v) { this.exigeProjetoFinal = v; }

    public Boolean getAprovacaoPorDisciplina() { return aprovacaoPorDisciplina; }
    public void setAprovacaoPorDisciplina(Boolean v) { this.aprovacaoPorDisciplina = v; }

    public Boolean getAprovacaoPorModulo() { return aprovacaoPorModulo; }
    public void setAprovacaoPorModulo(Boolean v) { this.aprovacaoPorModulo = v; }

    public String getConceitosPossiveis() { return conceitosPossiveis; }
    public void setConceitosPossiveis(String v) { this.conceitosPossiveis = v; }

    public String getConceitoMinimoAprovacao() { return conceitoMinimoAprovacao; }
    public void setConceitoMinimoAprovacao(String v) { this.conceitoMinimoAprovacao = v; }
}
