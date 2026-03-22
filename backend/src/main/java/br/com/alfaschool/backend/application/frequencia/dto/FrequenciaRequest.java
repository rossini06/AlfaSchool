package br.com.alfaschool.backend.application.frequencia.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record FrequenciaRequest(
        @NotNull(message = "Aluno é obrigatório")      UUID alunoId,
        @NotNull(message = "Turma é obrigatória")      UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId,
        @NotNull(message = "Data é obrigatória")       LocalDate data,
        Boolean presente,
        String obs
) {}
