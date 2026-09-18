package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tudo o que a TV precisa para montar a tela ao conectar e ao reconectar.
 *
 * A TV reconecta sozinha o dia inteiro; sem este endpoint ela voltaria com
 * a tela vazia ate' o proximo evento, que pode demorar meia hora.
 *
 * `retiradas` traz APENAS o recorte das fontes deste painel. O formato
 * (painel aninhado + lista `retiradas`) e' o que
 * `frontend/src/services/accessApi.js` consome.
 */
public record PainelEstadoResponse(
        PainelDaTela painel,
        Instant servidorEm,
        List<RetiradaFilaItem> retiradas
) {

    /**
     * `exibeFoto` e `retencaoSeg` sao politica de exibicao, nao decoracao:
     * a TV usa a retencao para tirar o cartao (e a foto) da tela depois da
     * liberacao.
     */
    public record PainelDaTela(
            UUID id,
            String nome,
            String slug,
            TipoPainel tipo,
            boolean exibeFoto,
            int retencaoSeg
    ) {
        public static PainelDaTela from(AccPainel p) {
            return new PainelDaTela(p.getId(), p.getNome(), p.getSlug(), p.getTipo(),
                    p.isExibeFoto(), p.getRetencaoSeg());
        }
    }
}
