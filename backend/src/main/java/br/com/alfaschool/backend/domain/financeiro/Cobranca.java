package br.com.alfaschool.backend.domain.financeiro;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cobrancas")
public class Cobranca extends BaseEntity {

    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 160)
    private String descricao;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(length = 20, nullable = false)
    private String status = "pendente";

    @Column(length = 7)
    private String competencia;

    public UUID getContratoId() { return contratoId; }
    public void setContratoId(UUID v) { this.contratoId = v; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal v) { this.valor = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate v) { this.vencimento = v; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate v) { this.dataPagamento = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String v) { this.competencia = v; }
}
