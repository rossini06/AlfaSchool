package br.com.alfaschool.backend.application.nota.dto;

import br.com.alfaschool.backend.domain.nota.Nota;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotaResponse(
        UUID id, UUID tenantId,
        UUID alunoId, UUID avaliacaoId,
        BigDecimal nota, String obs,
        Instant createdAt, Instant updatedAt
) {
    public static NotaResponse from(Nota n) {
        return new NotaResponse(
                n.getId(), n.getTenantId(),
                n.getAlunoId(), n.getAvaliacaoId(),
                n.getNota(), n.getObs(),
                n.getCreatedAt(), n.getUpdatedAt()
        );
    }
}
