package br.com.alfaschool.backend.application.saas.dto;

import br.com.alfaschool.backend.domain.saas.SaasPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaasPlanResponse(
    UUID id, UUID tenantId, String nome, String slug, String descricao,
    BigDecimal precoMensal, BigDecimal precoAnual,
    int maxEscolas, int maxUsuarios, int maxDispositivos,
    String recursos, boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static SaasPlanResponse from(SaasPlan p) {
        return new SaasPlanResponse(
            p.getId(), p.getTenantId(), p.getNome(), p.getSlug(), p.getDescricao(),
            p.getPrecoMensal(), p.getPrecoAnual(),
            p.getMaxEscolas(), p.getMaxUsuarios(), p.getMaxDispositivos(),
            p.getRecursos(), p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
