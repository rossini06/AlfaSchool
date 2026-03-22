package br.com.alfaschool.backend.domain.avaliacao;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao extends BaseEntity {

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "disciplina_id", nullable = false)
    private UUID disciplinaId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 40, nullable = false)
    private String tipo = "prova-escrita";

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso = BigDecimal.ONE;

    @Column(name = "data_avaliacao")
    private LocalDate dataAvaliacao;

    @Column(name = "nota_maxima", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaMaxima = BigDecimal.TEN;

    @Column(length = 20)
    private String periodo;

    @Column(name = "nota_minima", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaMinima = new BigDecimal("5.00");

    @Column(length = 20, nullable = false)
    private String status = "rascunho";

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String criterios;

    @Column(name = "data_entrega")
    private LocalDate dataEntrega;

    // --- getters / setters ---

    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID v) { this.turmaId = v; }

    public UUID getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(UUID v) { this.disciplinaId = v; }

    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }

    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }

    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal v) { this.peso = v; }

    public LocalDate getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDate v) { this.dataAvaliacao = v; }

    public BigDecimal getNotaMaxima() { return notaMaxima; }
    public void setNotaMaxima(BigDecimal v) { this.notaMaxima = v; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String v) { this.periodo = v; }

    public BigDecimal getNotaMinima() { return notaMinima; }
    public void setNotaMinima(BigDecimal v) { this.notaMinima = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }

    public String getCriterios() { return criterios; }
    public void setCriterios(String v) { this.criterios = v; }

    public LocalDate getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDate v) { this.dataEntrega = v; }
}
