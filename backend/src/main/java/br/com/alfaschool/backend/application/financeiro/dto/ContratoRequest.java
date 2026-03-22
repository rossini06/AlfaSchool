package br.com.alfaschool.backend.application.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoRequest(
        @NotNull(message = "Aluno é obrigatório") UUID alunoId,
        UUID responsavelId,
        @NotNull(message = "Plano é obrigatório") UUID planoId,
        UUID matriculaId,
        @NotNull(message = "Data de início é obrigatória") LocalDate dataInicio,
        LocalDate dataFim,
        String obs
) {}
