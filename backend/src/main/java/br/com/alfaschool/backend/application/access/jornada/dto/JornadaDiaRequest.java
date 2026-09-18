package br.com.alfaschool.backend.application.access.jornada.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** dia_semana 1=segunda ... 7=domingo (mesmo indice do java.time). */
public record JornadaDiaRequest(
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        boolean frequenta,
        LocalTime entradaPrevista,
        LocalTime saidaPrevista,
        @Min(0) @Max(1440) Integer cargaMinutos
) {
}
