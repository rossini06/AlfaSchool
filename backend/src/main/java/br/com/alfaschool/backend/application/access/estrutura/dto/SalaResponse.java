package br.com.alfaschool.backend.application.access.estrutura.dto;

import br.com.alfaschool.backend.domain.access.estrutura.AccSala;

import java.time.Instant;
import java.util.UUID;

public record SalaResponse(
        UUID id, UUID tenantId, UUID unitId, UUID zonaId, String nome, String codigo,
        String bloco, String andar, Integer capacidade, boolean ativo,
        Instant createdAt, Instant updatedAt
) {
    public static SalaResponse from(AccSala s) {
        return new SalaResponse(s.getId(), s.getTenantId(), s.getUnitId(), s.getZonaId(),
                s.getNome(), s.getCodigo(), s.getBloco(), s.getAndar(), s.getCapacidade(),
                s.isAtivo(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
