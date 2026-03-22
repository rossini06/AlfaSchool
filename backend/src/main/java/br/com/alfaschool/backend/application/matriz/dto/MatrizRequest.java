package br.com.alfaschool.backend.application.matriz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatrizRequest(
        @NotNull(message = "Curso é obrigatório")
        UUID cursoId,

        @NotNull(message = "Disciplina é obrigatória")
        UUID disciplinaId,

        @NotBlank(message = "Período é obrigatório")
        String periodo,

        Integer cargaHoraria,
        Boolean obrigatoria
) {}
