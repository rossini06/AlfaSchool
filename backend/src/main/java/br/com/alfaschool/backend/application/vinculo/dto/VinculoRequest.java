package br.com.alfaschool.backend.application.vinculo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VinculoRequest(
        @NotNull(message = "Professor é obrigatório") UUID professorId,
        @NotNull(message = "Turma é obrigatória")    UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId
) {}
