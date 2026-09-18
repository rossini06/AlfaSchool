package br.com.alfaschool.backend.application.access.estrutura.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TurmaSalaRequest(
        @NotNull(message = "turmaId e obrigatorio") UUID turmaId,
        @NotNull(message = "salaId e obrigatorio") UUID salaId,
        @NotNull(message = "vigenciaInicio e obrigatoria") LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        // ISO local time: aceita "08:00" e "08:00:00".
        LocalTime horaInicio,
        LocalTime horaFim,
        // CSV ISO-8601: 1=segunda ... 7=domingo. Vazio vale a semana inteira.
        @Pattern(regexp = "^\\s*$|^[1-7](\\s*,\\s*[1-7])*$",
                message = "diasSemana deve ser um CSV de 1 a 7, ex.: 1,2,3,4,5")
        String diasSemana
) {}
