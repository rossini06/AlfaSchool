package br.com.alfaschool.backend.application.matriz.dto;

import br.com.alfaschool.backend.domain.matriz.MatrizCurricular;
import java.time.Instant;
import java.util.UUID;

public record MatrizResponse(
        UUID id,
        UUID tenantId,
        UUID cursoId,
        UUID disciplinaId,
        String periodo,
        Integer cargaHoraria,
        boolean obrigatoria,
        Instant createdAt,
        Instant updatedAt
) {
    public static MatrizResponse from(MatrizCurricular m) {
        return new MatrizResponse(
                m.getId(), m.getTenantId(), m.getCursoId(), m.getDisciplinaId(),
                m.getPeriodo(), m.getCargaHoraria(), m.isObrigatoria(),
                m.getCreatedAt(), m.getUpdatedAt()
        );
    }
}
