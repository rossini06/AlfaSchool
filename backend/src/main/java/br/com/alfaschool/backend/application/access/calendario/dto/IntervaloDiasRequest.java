package br.com.alfaschool.backend.application.access.calendario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Lancamento em lote. Sem isso, cadastrar o recesso de 20/12 a 31/01 seriam
 * 43 chamadas na mao e a secretaria simplesmente nao faria.
 */
public record IntervaloDiasRequest(
        @NotNull(message = "dataInicio e obrigatoria") LocalDate dataInicio,
        @NotNull(message = "dataFim e obrigatoria") LocalDate dataFim,
        @NotBlank(message = "tipo e obrigatorio") String tipo,
        @Size(max = 255) String descricao,
        // Recesso pega o intervalo inteiro; ja "semana de provas" so os uteis.
        Boolean apenasDiasUteis,
        // Falso protege lancamentos ja feitos (um feriado dentro do recesso).
        Boolean sobrescrever
) {}
