package br.com.alfaschool.backend.application.curso.dto;

import br.com.alfaschool.backend.domain.curso.Curso;
import java.time.Instant;
import java.util.UUID;

public record CursoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String codigo,
    String descricao, Integer cargaHoraria, String modalidade, String nivel,
    boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static CursoResponse from(Curso c) {
        return new CursoResponse(c.getId(), c.getTenantId(), c.getUnitId(),
            c.getNome(), c.getCodigo(), c.getDescricao(), c.getCargaHoraria(),
            c.getModalidade(), c.getNivel(), c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
