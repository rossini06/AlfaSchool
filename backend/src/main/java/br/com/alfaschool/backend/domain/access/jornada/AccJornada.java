package br.com.alfaschool.backend.domain.access.jornada;

import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;

/**
 * Jornada contratada: o pacote de horas que a familia comprou.
 *
 * Nao confundir com frequencia pedagogica. Um aluno pode ficar 10h na
 * escola e ter 4h de aula: aqui interessa o tempo de PERMANENCIA, porque
 * e' ele que vira cobranca de excedente.
 */
@Entity
@Table(name = "acc_jornadas")
public class AccJornada extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Franquia de atraso na ENTRADA. Nao entra na conta de excedente. */
    @Column(name = "tolerancia_entrada_min", nullable = false)
    private int toleranciaEntradaMin = 0;

    /** Franquia contratual antes de comecar a cobrar excedente. */
    @Column(name = "tolerancia_saida_min", nullable = false)
    private int toleranciaSaidaMin = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "regra_excedente", nullable = false, length = 20)
    private RegraExcedente regraExcedente = RegraExcedente.HORARIO;

    @Column(nullable = false)
    private boolean ativo = true;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getToleranciaEntradaMin() { return toleranciaEntradaMin; }
    public void setToleranciaEntradaMin(int v) { this.toleranciaEntradaMin = v; }
    public int getToleranciaSaidaMin() { return toleranciaSaidaMin; }
    public void setToleranciaSaidaMin(int v) { this.toleranciaSaidaMin = v; }
    public RegraExcedente getRegraExcedente() { return regraExcedente; }
    public void setRegraExcedente(RegraExcedente v) { this.regraExcedente = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
