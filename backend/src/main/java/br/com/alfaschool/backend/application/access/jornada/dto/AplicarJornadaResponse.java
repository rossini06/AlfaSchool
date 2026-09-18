package br.com.alfaschool.backend.application.access.jornada.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da aplicacao em lote. Os ignorados voltam com o motivo: numa
 * turma de 30 e normal que dois alunos ja tenham um plano especial, e a
 * secretaria precisa saber quais para tratar a mao — falhar a operacao
 * inteira por causa de dois faria a escola desistir do lote.
 */
public record AplicarJornadaResponse(
        UUID jornadaId,
        int aplicados,
        List<AlunoJornadaResponse> vinculos,
        List<Ignorado> ignorados
) {
    public record Ignorado(UUID alunoId, String motivo) {
    }
}
