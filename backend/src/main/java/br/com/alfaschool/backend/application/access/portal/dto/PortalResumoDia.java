package br.com.alfaschool.backend.application.access.portal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resumo do dia de um aluno para a familia.
 *
 * @param jornadaContratadaMinutos jornada que a familia paga.
 * @param permanenciaMinutos       tempo ja cumprido hoje.
 * @param percentualCumprido       0..100+ (pode passar de 100 em dia estendido).
 */
public record PortalResumoDia(
        UUID alunoId,
        String alunoNome,
        LocalDate data,
        Instant entrada,
        Instant saida,
        boolean presenteAgora,
        Integer jornadaContratadaMinutos,
        Integer permanenciaMinutos,
        Double percentualCumprido,
        List<PortalEventoResumo> ultimosEventos
) {
}
