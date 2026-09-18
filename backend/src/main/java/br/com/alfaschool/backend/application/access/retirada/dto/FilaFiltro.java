package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.util.List;
import java.util.UUID;

/**
 * Filtros da fila. Todos opcionais.
 *
 * `esperandoHaMinutos` e' o filtro que a coordenacao mais usa: mostra quem
 * ja' passou do tempo aceitavel de espera.
 */
public record FilaFiltro(
        UUID unitId,
        UUID turmaId,
        UUID salaId,
        UUID portariaId,
        List<StatusRetirada> status,
        Integer esperandoHaMinutos
) {
    public static FilaFiltro vazio() {
        return new FilaFiltro(null, null, null, null, null, null);
    }
}
