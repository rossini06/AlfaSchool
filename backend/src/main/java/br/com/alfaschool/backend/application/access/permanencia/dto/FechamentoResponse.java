package br.com.alfaschool.backend.application.access.permanencia.dto;

import br.com.alfaschool.backend.domain.access.permanencia.AccFechamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FechamentoResponse(
        UUID id,
        UUID unitId,
        String competencia,
        LocalDate dataInicio,
        LocalDate dataFim,
        String status,
        UUID fechadoPor,
        Instant fechadoEm,
        int presencasAfetadas
) {
    public static FechamentoResponse from(AccFechamento f, int presencasAfetadas) {
        return new FechamentoResponse(f.getId(), f.getUnitId(), f.getCompetencia(), f.getDataInicio(),
                f.getDataFim(), f.getStatus(), f.getFechadoPor(), f.getFechadoEm(), presencasAfetadas);
    }
}
