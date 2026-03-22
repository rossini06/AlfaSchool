package br.com.alfaschool.backend.application.nota.dto;

import br.com.alfaschool.backend.domain.nota.Nota;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotaResponse(
        UUID id, UUID tenantId,
        UUID alunoId, UUID matriculaId, UUID avaliacaoId,
        BigDecimal nota, BigDecimal notaRecuperacao, BigDecimal notaFinal,
        String obs,
        Instant createdAt, Instant updatedAt
) {
    public static NotaResponse from(Nota n) {
        return new NotaResponse(
                n.getId(), n.getTenantId(),
                n.getAlunoId(), n.getMatriculaId(), n.getAvaliacaoId(),
                n.getNota(), n.getNotaRecuperacao(), n.getNotaFinal(),
                n.getObs(),
                n.getCreatedAt(), n.getUpdatedAt()
        );
    }
}
