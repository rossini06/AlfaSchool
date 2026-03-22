package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.StatusFrequencia;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO para lançamento de frequência em lote (toda a turma de uma vez)
 */
public record FrequenciaLoteRequest(
        @NotNull(message = "Turma é obrigatória") UUID turmaId,
        @NotNull(message = "Disciplina é obrigatória") UUID disciplinaId,
        @NotNull(message = "Data é obrigatória") LocalDate data,
        Integer numeroAula,
        @NotNull(message = "Frequências são obrigatórias") List<FrequenciaAlunoItem> frequencias
) {
    public record FrequenciaAlunoItem(
            @NotNull UUID alunoId,
            UUID matriculaId,
            StatusFrequencia status,
            String obs
    ) {}
}
