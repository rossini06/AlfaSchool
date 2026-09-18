package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.jornada.AccAlunoJornada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoJornadaResponse(
        UUID id,
        UUID alunoId,
        UUID jornadaId,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        String observacao,
        Instant createdAt,
        Instant updatedAt
) {
    public static AlunoJornadaResponse from(AccAlunoJornada v) {
        return new AlunoJornadaResponse(v.getId(), v.getAlunoId(), v.getJornadaId(),
                v.getVigenciaInicio(), v.getVigenciaFim(), v.getObservacao(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
