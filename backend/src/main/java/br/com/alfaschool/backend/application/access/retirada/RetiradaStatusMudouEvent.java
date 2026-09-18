package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.util.UUID;

/**
 * Uma retirada mudou de status.
 *
 * Carrega o recorte (unidade, turma, sala, portaria) porque e' ele que
 * decide em quais TVs o cartao aparece. Quem escuta nao precisa voltar ao
 * banco so' para saber onde publicar.
 *
 * Publicado dentro da transacao e consumido em AFTER_COMMIT: a tela nunca
 * pode mostrar o que o banco desfez.
 */
public record RetiradaStatusMudouEvent(
        UUID tenantId,
        UUID retiradaId,
        UUID unitId,
        UUID alunoId,
        UUID turmaId,
        UUID salaId,
        UUID portariaId,
        StatusRetirada statusAnterior,
        StatusRetirada statusNovo
) {

    /** Nome do evento SSE que a TV escuta. */
    public String nomeSse() {
        return switch (statusNovo) {
            case SOLICITADA -> "retirada.aberta";
            case PREPARANDO -> "retirada.preparando";
            case PRONTO -> "retirada.pronto";
            case ENTREGUE -> "retirada.entregue";
            // Cancelada e negada tiram o cartao da tela do mesmo jeito; a
            // TV nao precisa de dois tratamentos para o mesmo desfecho.
            case CANCELADA, NEGADA -> "retirada.cancelada";
        };
    }
}
