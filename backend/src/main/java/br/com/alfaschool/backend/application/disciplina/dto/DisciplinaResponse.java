package br.com.alfaschool.backend.application.disciplina.dto;

import br.com.alfaschool.backend.domain.disciplina.Disciplina;
import java.time.Instant;
import java.util.UUID;

public record DisciplinaResponse(
        UUID id,
        UUID tenantId,
        String nome,
        String codigo,
        Integer cargaHoraria,
        String descricao,
        boolean ativa,
        Instant createdAt,
        Instant updatedAt
) {
    public static DisciplinaResponse from(Disciplina d) {
        return new DisciplinaResponse(
                d.getId(), d.getTenantId(), d.getNome(), d.getCodigo(),
                d.getCargaHoraria(), d.getDescricao(), d.isAtiva(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
