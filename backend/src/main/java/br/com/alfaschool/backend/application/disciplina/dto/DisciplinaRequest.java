package br.com.alfaschool.backend.application.disciplina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplinaRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 255)
        String nome,
        String codigo,
        Integer cargaHoraria,
        String descricao,
        Boolean ativa
) {}
