package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param segredo segredo do provedor em CLARO. Entra aqui, e' cifrado na
 *                gravacao e nunca mais sai. Nulo = manter o que ja esta
 *                gravado; string vazia = apagar.
 */
public record NotificacaoConfigRequest(
        @NotNull CanalNotificacao canal,
        @Size(max = 40) String provider,
        @Size(max = 160) String remetente,
        String configJson,
        String segredo,
        Integer limiteDiario,
        Boolean ativo
) {
}
