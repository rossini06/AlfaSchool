package br.com.alfaschool.backend.application.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PlanoFinanceiroRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser positivo") BigDecimal valor,
        String periodicidade,
        String descricao,
        Boolean ativo
) {}
