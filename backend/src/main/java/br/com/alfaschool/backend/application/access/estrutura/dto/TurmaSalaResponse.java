package br.com.alfaschool.backend.application.access.estrutura.dto;

import br.com.alfaschool.backend.domain.access.estrutura.AccTurmaSala;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TurmaSalaResponse(
        UUID id, UUID tenantId, UUID turmaId, UUID salaId,
        LocalDate vigenciaInicio, LocalDate vigenciaFim,
        LocalTime horaInicio, LocalTime horaFim,
        String diasSemana, List<Integer> diasSemanaResolvidos,
        int especificidade, Instant createdAt, Instant updatedAt
) {
    public static TurmaSalaResponse from(AccTurmaSala v) {
        return new TurmaSalaResponse(
                v.getId(), v.getTenantId(), v.getTurmaId(), v.getSalaId(),
                v.getVigenciaInicio(), v.getVigenciaFim(), v.getHoraInicio(), v.getHoraFim(),
                v.getDiasSemana(), List.copyOf(v.diasSemanaResolvidos()), v.especificidade(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
