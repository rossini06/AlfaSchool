package br.com.alfaschool.backend.application.disciplina.dto;

import br.com.alfaschool.backend.domain.disciplina.Disciplina;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisciplinaResponse(
        UUID id,
        UUID tenantId,
        String nome,
        String codigo,
        Integer cargaHoraria,
        String descricao,
        UUID cursoId,
        String tipo,
        BigDecimal notaMaxima,
        BigDecimal peso,
        Boolean permiteRecuperacao,
        String tipoAvaliacao,
        boolean ativa,
        Instant createdAt,
        Instant updatedAt
) {
    public static DisciplinaResponse from(Disciplina d) {
        return new DisciplinaResponse(
                d.getId(), d.getTenantId(), d.getNome(), d.getCodigo(),
                d.getCargaHoraria(), d.getDescricao(), d.getCursoId(),
                d.getTipo(), d.getNotaMaxima(), d.getPeso(),
                d.getPermiteRecuperacao(), d.getTipoAvaliacao(),
                d.isAtiva(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
