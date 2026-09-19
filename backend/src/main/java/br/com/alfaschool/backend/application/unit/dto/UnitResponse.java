package br.com.alfaschool.backend.application.unit.dto;

import br.com.alfaschool.backend.domain.tenant.Unit;
import java.time.Instant;
import java.util.UUID;

public record UnitResponse(
    UUID id, UUID tenantId, String name, String address, String city, String state,
    String cep, String email, String telefone,
    boolean active, Instant createdAt, Instant updatedAt
) {
    public static UnitResponse from(Unit u) {
        return new UnitResponse(
            u.getId(), u.getTenantId(), u.getName(), u.getAddress(),
            u.getCity(), u.getState(), u.getCep(), u.getEmail(), u.getTelefone(),
            u.isActive(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}
