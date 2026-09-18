package br.com.alfaschool.backend.application.access.permanencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Correcao manual de um par entrada/saida.
 *
 * O motivo e' OBRIGATORIO: este ajuste muda o valor de uma fatura, e
 * meses depois alguem vai perguntar por que aquele dia tem 40 minutos a
 * mais. Sem o motivo gravado, a resposta nao existe.
 *
 * @param parId nulo = incluir um par novo; preenchido = corrigir ou
 *              remover o par existente.
 * @param remover true remove o par apontado por {@code parId}.
 */
public record AjusteParRequest(
        @NotNull UUID alunoId,
        @NotNull LocalDate data,
        UUID parId,
        Instant entradaEm,
        Instant saidaEm,
        boolean remover,
        @NotNull @Size(min = 5, max = 255) String motivo
) {
}
