package br.com.alfaschool.backend.application.nota.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record NotaRequest(
        @NotNull(message = "Aluno é obrigatório")     UUID alunoId,
        @NotNull(message = "Avaliação é obrigatória") UUID avaliacaoId,
        @DecimalMin(value = "0.0", message = "Nota mínima é 0")
        BigDecimal nota,
        String obs
) {}
