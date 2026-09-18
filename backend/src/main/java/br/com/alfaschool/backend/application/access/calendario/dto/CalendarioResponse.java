package br.com.alfaschool.backend.application.access.calendario.dto;

import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;

import java.time.Instant;
import java.util.UUID;

public record CalendarioResponse(
        UUID id, UUID tenantId, UUID unitId, String nome, Integer anoLetivo,
        boolean ativo, boolean global, Instant createdAt, Instant updatedAt
) {
    public static CalendarioResponse from(AccCalendario c) {
        return new CalendarioResponse(c.getId(), c.getTenantId(), c.getUnitId(), c.getNome(),
                c.getAnoLetivo(), c.isAtivo(), c.getUnitId() == null,
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
