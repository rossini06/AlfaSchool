package br.com.alfaschool.backend.application.access.portal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PortalPermanenciaDia(
        LocalDate data,
        Instant entrada,
        Instant saida,
        Integer permanenciaMinutos,
        Integer jornadaContratadaMinutos,
        Double percentualCumprido
) {
}
