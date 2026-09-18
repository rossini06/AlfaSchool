package br.com.alfaschool.backend.application.access.permanencia.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Recalculo forcado de um periodo.
 *
 * @param alunoId nulo recalcula todo mundo com jornada ou presenca no
 *                periodo.
 * @param incluirAjustadas por padrao FALSO: o recalculo reconstroi o dia
 *                a partir dos eventos e apagaria a correcao que alguem
 *                fez a mao. So' quem sabe o que esta fazendo liga isso.
 */
public record RecalculoRequest(
        @NotNull LocalDate inicio,
        @NotNull LocalDate fim,
        UUID alunoId,
        boolean incluirAjustadas
) {
}
