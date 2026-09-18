package br.com.alfaschool.backend.application.access.jornada.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AlunoJornadaRequest(
        @NotNull UUID alunoId,
        @NotNull UUID jornadaId,
        @NotNull LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        @Size(max = 255) String observacao
) {
}
