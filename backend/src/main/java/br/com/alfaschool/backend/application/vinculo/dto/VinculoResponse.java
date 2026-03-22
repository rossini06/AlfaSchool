package br.com.alfaschool.backend.application.vinculo.dto;

import br.com.alfaschool.backend.domain.vinculo.ProfessorTurmaDisciplina;
import java.time.Instant;
import java.util.UUID;

public record VinculoResponse(
        UUID id,
        UUID tenantId,
        UUID professorId,
        UUID turmaId,
        UUID disciplinaId,
        Instant createdAt
) {
    public static VinculoResponse from(ProfessorTurmaDisciplina v) {
        return new VinculoResponse(
                v.getId(), v.getTenantId(),
                v.getProfessorId(), v.getTurmaId(), v.getDisciplinaId(),
                v.getCreatedAt()
        );
    }
}
