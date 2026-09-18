package br.com.alfaschool.backend.application.access.retirada.dto;

import java.time.Instant;

/**
 * Saida efetiva informada a mao, para escola sem catraca de saida.
 *
 * `momento` permite corrigir o horario quando a coordenacao registra
 * minutos depois; nulo significa agora.
 */
public record RegistrarSaidaRequest(
        Instant momento,
        String observacao
) {
}
