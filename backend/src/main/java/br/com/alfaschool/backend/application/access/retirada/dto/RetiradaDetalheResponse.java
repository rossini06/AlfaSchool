package br.com.alfaschool.backend.application.access.retirada.dto;

import java.util.List;

/** Detalhe da retirada com a trilha completa de quem mudou o que e quando. */
public record RetiradaDetalheResponse(
        RetiradaFilaItem retirada,
        List<RetiradaHistoricoResponse> historico
) {
}
