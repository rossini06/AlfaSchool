package br.com.alfaschool.backend.application.access.permanencia.dto;

import java.time.LocalDate;

/**
 * @param diasIgnorados dias pulados por estarem congelados ou ajustados a
 *                      mao — o numero importa: se vier alto, o operador
 *                      esta tentando recalcular um periodo ja faturado.
 */
public record RecalculoResponse(
        LocalDate inicio,
        LocalDate fim,
        int alunos,
        int diasRecalculados,
        int diasIgnorados
) {
}
