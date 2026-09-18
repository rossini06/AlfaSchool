package br.com.alfaschool.backend.application.access.calendario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalendarioDiaRequest(
        @NotNull(message = "data e obrigatoria") LocalDate data,
        // String e nao enum: valor invalido vira 400 com mensagem nossa.
        @NotBlank(message = "tipo e obrigatorio") String tipo,
        @Size(max = 255) String descricao
) {}
