package br.com.alfaschool.backend.application.access.calendario.dto;

import br.com.alfaschool.backend.domain.access.calendario.AccCalendarioDia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarioDiaResponse(
        UUID id, UUID tenantId, UUID calendarioId, LocalDate data,
        String tipo, String descricao, Instant createdAt, Instant updatedAt
) {
    public static CalendarioDiaResponse from(AccCalendarioDia d) {
        return new CalendarioDiaResponse(d.getId(), d.getTenantId(), d.getCalendarioId(), d.getData(),
                d.getTipo() != null ? d.getTipo().name() : null, d.getDescricao(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
