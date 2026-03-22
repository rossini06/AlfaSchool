package br.com.alfaschool.backend.application.financeiro.dto;

import br.com.alfaschool.backend.domain.financeiro.Contrato;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResponse(
        UUID id, UUID tenantId,
        UUID alunoId, UUID responsavelId, UUID planoId, UUID matriculaId,
        LocalDate dataInicio, LocalDate dataFim,
        String status, String obs,
        Instant createdAt, Instant updatedAt
) {
    public static ContratoResponse from(Contrato c) {
        return new ContratoResponse(
                c.getId(), c.getTenantId(),
                c.getAlunoId(), c.getResponsavelId(), c.getPlanoId(), c.getMatriculaId(),
                c.getDataInicio(), c.getDataFim(),
                c.getStatus(), c.getObs(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
