package br.com.alfaschool.backend.application.frequencia.dto;

import br.com.alfaschool.backend.domain.frequencia.Frequencia;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FrequenciaResponse(
        UUID id, UUID tenantId,
        UUID alunoId, UUID turmaId, UUID disciplinaId,
        LocalDate data, boolean presente, String obs,
        Instant createdAt, Instant updatedAt
) {
    public static FrequenciaResponse from(Frequencia f) {
        return new FrequenciaResponse(
                f.getId(), f.getTenantId(),
                f.getAlunoId(), f.getTurmaId(), f.getDisciplinaId(),
                f.getData(), f.isPresente(), f.getObs(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }
}
