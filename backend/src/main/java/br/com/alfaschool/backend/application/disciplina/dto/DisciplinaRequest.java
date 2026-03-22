package br.com.alfaschool.backend.application.disciplina.dto;

import jakarta.validation.constraints.NotBlank;

public record DisciplinaRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String codigo,
        Integer cargaHoraria,
        String descricao,
        Boolean ativa
) {}
