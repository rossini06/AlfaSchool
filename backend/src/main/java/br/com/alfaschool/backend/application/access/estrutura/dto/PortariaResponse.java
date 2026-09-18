package br.com.alfaschool.backend.application.access.estrutura.dto;

import br.com.alfaschool.backend.domain.access.estrutura.AccPortaria;

import java.time.Instant;
import java.util.UUID;

public record PortariaResponse(
        UUID id, UUID tenantId, UUID unitId, String nome, String tipo,
        String descricao, boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static PortariaResponse from(AccPortaria p) {
        return new PortariaResponse(
                p.getId(), p.getTenantId(), p.getUnitId(), p.getNome(),
                p.getTipo() != null ? p.getTipo().name() : null,
                p.getDescricao(), p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
