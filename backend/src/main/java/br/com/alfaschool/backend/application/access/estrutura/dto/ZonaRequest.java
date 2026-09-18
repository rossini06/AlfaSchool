package br.com.alfaschool.backend.application.access.estrutura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ZonaRequest(
        @NotNull(message = "unitId e obrigatorio") UUID unitId,
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        @Size(max = 255) String descricao,
        Boolean ativo
) {}
