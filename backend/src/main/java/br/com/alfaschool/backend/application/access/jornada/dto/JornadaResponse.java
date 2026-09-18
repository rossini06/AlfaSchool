package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.jornada.AccJornada;
import br.com.alfaschool.backend.domain.access.jornada.AccJornadaDia;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record JornadaResponse(
        UUID id,
        UUID tenantId,
        String nome,
        String descricao,
        int toleranciaEntradaMin,
        int toleranciaSaidaMin,
        RegraExcedente regraExcedente,
        boolean ativo,
        List<JornadaDiaResponse> dias,
        Instant createdAt,
        Instant updatedAt
) {
    public static JornadaResponse from(AccJornada j, List<AccJornadaDia> dias) {
        List<JornadaDiaResponse> linhas = dias == null ? List.of() : dias.stream()
                .sorted(Comparator.comparingInt(AccJornadaDia::getDiaSemana))
                .map(JornadaDiaResponse::from)
                .toList();
        return new JornadaResponse(j.getId(), j.getTenantId(), j.getNome(), j.getDescricao(),
                j.getToleranciaEntradaMin(), j.getToleranciaSaidaMin(), j.getRegraExcedente(),
                j.isAtivo(), linhas, j.getCreatedAt(), j.getUpdatedAt());
    }

    public static JornadaResponse from(AccJornada j) {
        return from(j, List.of());
    }
}
