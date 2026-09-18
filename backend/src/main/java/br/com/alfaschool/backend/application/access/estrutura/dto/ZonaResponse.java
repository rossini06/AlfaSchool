package br.com.alfaschool.backend.application.access.estrutura.dto;

import br.com.alfaschool.backend.domain.access.estrutura.AccZona;

import java.time.Instant;
import java.util.UUID;

public record ZonaResponse(
        UUID id, UUID tenantId, UUID unitId, String nome, String descricao,
        boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static ZonaResponse from(AccZona z) {
        return new ZonaResponse(z.getId(), z.getTenantId(), z.getUnitId(), z.getNome(),
                z.getDescricao(), z.isAtivo(), z.getCreatedAt(), z.getUpdatedAt());
    }
}
