package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.ConteudoMinistrado;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConteudoMinistradoResponse(
        UUID id,
        UUID tenantId,
        UUID turmaId,
        UUID disciplinaId,
        UUID professorId,
        LocalDate data,
        String descricao,
        String objetivos,
        String recursos,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConteudoMinistradoResponse from(ConteudoMinistrado c) {
        return new ConteudoMinistradoResponse(
                c.getId(),
                c.getTenantId(),
                c.getTurmaId(),
                c.getDisciplinaId(),
                c.getProfessorId(),
                c.getData(),
                c.getDescricao(),
                c.getObjetivos(),
                c.getRecursos(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
