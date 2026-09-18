package br.com.alfaschool.backend.application.access.calendario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CalendarioRequest(
        // Nulo de proposito: calendario global do tenant, valido para toda a escola.
        UUID unitId,
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        @NotNull(message = "anoLetivo e obrigatorio")
        @Min(value = 2000, message = "anoLetivo invalido")
        @Max(value = 2100, message = "anoLetivo invalido") Integer anoLetivo,
        Boolean ativo
) {}
