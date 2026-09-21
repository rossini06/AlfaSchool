package br.com.alfaschool.backend.application.tenant.dto;

import br.com.alfaschool.backend.domain.tenant.Tenant;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TenantResponse(
    UUID id, UUID tenantId, String name, String document, boolean active,
    /** O tenant mestre e' da Alfa, nao de uma escola: a tela nao deixa editar. */
    boolean mestre,
    /** Codigos dos modulos vigentes, para a lista mostrar sem abrir cada rede. */
    List<String> modulos,
    Instant createdAt, Instant updatedAt
) {
    public static final String DOCUMENTO_DO_MESTRE = "MASTER";

    public static TenantResponse from(Tenant t) {
        return from(t, List.of());
    }

    public static TenantResponse from(Tenant t, List<String> modulos) {
        return new TenantResponse(
            t.getId(), t.getTenantId(), t.getName(), t.getDocument(), t.isActive(),
            DOCUMENTO_DO_MESTRE.equals(t.getDocument()), modulos,
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
