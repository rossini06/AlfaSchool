package br.com.alfaschool.backend.application.diario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ConteudoMinistradoRequest(
        @NotNull(message = "Turma é obrigatória") UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId,
        UUID professorId,
        @NotNull(message = "Data é obrigatória") LocalDate data,
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        String objetivos,
        String recursos
) {}
