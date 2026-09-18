package br.com.alfaschool.backend.application.access.estrutura.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SalaRequest(
        @NotNull(message = "unitId e obrigatorio") UUID unitId,
        UUID zonaId,
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        @Size(max = 30) String codigo,
        @Size(max = 60) String bloco,
        @Size(max = 30) String andar,
        @Min(value = 1, message = "capacidade deve ser maior que zero") Integer capacidade,
        Boolean ativo
) {}
