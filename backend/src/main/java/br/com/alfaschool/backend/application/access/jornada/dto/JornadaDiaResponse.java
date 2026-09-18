package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.jornada.AccJornadaDia;

import java.time.LocalTime;
import java.util.UUID;

public record JornadaDiaResponse(
        UUID id,
        int diaSemana,
        boolean frequenta,
        LocalTime entradaPrevista,
        LocalTime saidaPrevista,
        int cargaMinutos
) {
    public static JornadaDiaResponse from(AccJornadaDia d) {
        return new JornadaDiaResponse(d.getId(), d.getDiaSemana(), d.isFrequenta(),
                d.getEntradaPrevista(), d.getSaidaPrevista(), d.getCargaMinutos());
    }
}
