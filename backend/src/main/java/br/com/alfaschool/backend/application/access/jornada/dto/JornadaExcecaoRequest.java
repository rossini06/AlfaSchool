package br.com.alfaschool.backend.application.access.jornada.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record JornadaExcecaoRequest(
        @NotNull UUID alunoId,
        @NotNull LocalDate data,
        boolean frequenta,
        LocalTime entradaPrevista,
        LocalTime saidaPrevista,
        @Min(0) @Max(1440) Integer cargaMinutos,
        @Size(max = 255) String motivo
) {
}
