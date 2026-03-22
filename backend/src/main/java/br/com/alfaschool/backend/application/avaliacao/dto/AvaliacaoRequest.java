package br.com.alfaschool.backend.application.avaliacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoRequest(
        @NotNull(message = "Turma é obrigatória")      UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId,
        @NotBlank(message = "Nome é obrigatório")      String nome,
        String tipo,
        BigDecimal peso,
        LocalDate dataAvaliacao,
        BigDecimal notaMaxima
) {}
