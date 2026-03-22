package br.com.alfaschool.backend.application.frequencia.dto;

import br.com.alfaschool.backend.domain.diario.StatusFrequencia;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record FrequenciaRequest(
        @NotNull(message = "Aluno é obrigatório")      UUID alunoId,
        UUID matriculaId,
        @NotNull(message = "Turma é obrigatória")      UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId,
        @NotNull(message = "Data é obrigatória")       LocalDate data,
        Integer numeroAula,
        Boolean presente,
        StatusFrequencia status,
        String obs
) {}
