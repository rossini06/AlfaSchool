package br.com.alfaschool.backend.application.access.estrutura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PortariaRequest(
        @NotNull(message = "unitId e obrigatorio") UUID unitId,
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        // String e nao enum: assim um valor invalido vira 400 com mensagem
        // nossa em portugues, em vez do erro cru de desserializacao do Jackson.
        String tipo,
        @Size(max = 255) String descricao,
        Boolean ativo
) {}
