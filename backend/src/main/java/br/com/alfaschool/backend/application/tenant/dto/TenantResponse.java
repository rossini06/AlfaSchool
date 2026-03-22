package br.com.alfaschool.backend.application.tenant.dto;

import br.com.alfaschool.backend.domain.tenant.Tenant;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
    UUID id, UUID tenantId, String name, String document, boolean active,
    Instant createdAt, Instant updatedAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
            t.getId(), t.getTenantId(), t.getName(), t.getDocument(), t.isActive(),
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
