package br.com.alfaschool.backend.application.access.portal.dto;

import java.time.Instant;

/**
 * Linha do historico de movimentacao mostrada a familia.
 *
 * <p>Sem foto e sem identificador de dispositivo: a familia precisa saber que
 * entrou e a que horas, nao qual catraca nem qual biometria casou.
 */
public record PortalEventoResumo(
        Instant ocorridoEm,
        String sentido,
        String local
) {
}
