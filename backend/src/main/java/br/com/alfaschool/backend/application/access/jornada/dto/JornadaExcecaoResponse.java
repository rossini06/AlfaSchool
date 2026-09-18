package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.jornada.AccJornadaExcecao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record JornadaExcecaoResponse(
        UUID id,
        UUID alunoId,
        LocalDate data,
        boolean frequenta,
        LocalTime entradaPrevista,
        LocalTime saidaPrevista,
        Integer cargaMinutos,
        String motivo,
        Instant createdAt,
        Instant updatedAt
) {
    public static JornadaExcecaoResponse from(AccJornadaExcecao e) {
        return new JornadaExcecaoResponse(e.getId(), e.getAlunoId(), e.getData(), e.isFrequenta(),
                e.getEntradaPrevista(), e.getSaidaPrevista(), e.getCargaMinutos(), e.getMotivo(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
