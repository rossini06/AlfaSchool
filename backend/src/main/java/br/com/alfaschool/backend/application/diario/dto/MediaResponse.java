package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.Media;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        UUID tenantId,
        UUID matriculaId,
        UUID disciplinaId,
        UUID turmaId,
        String periodo,
        BigDecimal media,
        BigDecimal percentualFrequencia,
        Integer totalAulas,
        Integer totalPresencas,
        Integer totalFaltas,
        Integer totalJustificadas,
        SituacaoAluno situacao,
        String conceito,
        String observacaoDescritiva,
        Instant createdAt,
        Instant updatedAt
) {
    public static MediaResponse from(Media m) {
        return new MediaResponse(
                m.getId(),
                m.getTenantId(),
                m.getMatriculaId(),
                m.getDisciplinaId(),
                m.getTurmaId(),
                m.getPeriodo(),
                m.getMedia(),
                m.getPercentualFrequencia(),
                m.getTotalAulas(),
                m.getTotalPresencas(),
                m.getTotalFaltas(),
                m.getTotalJustificadas(),
                m.getSituacao(),
                m.getConceito(),
                m.getObservacaoDescritiva(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
