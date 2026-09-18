package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de configuracao de canal.
 *
 * <p>NAO EXISTE campo com o segredo. Nem cifrado, nem parcial. O unico sinal e'
 * {@code segredoConfigurado} e uma mascara fixa: se um token da Meta vazasse
 * por uma tela de configuracao, qualquer pessoa com acesso administrativo
 * passaria a poder mandar WhatsApp em nome da escola.
 */
public record NotificacaoConfigResponse(
        UUID id,
        UUID tenantId,
        CanalNotificacao canal,
        String provider,
        String remetente,
        String configJson,
        boolean segredoConfigurado,
        String segredoMascarado,
        Integer limiteDiario,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificacaoConfigResponse from(AccNotificacaoConfig c) {
        boolean temSegredo = c.temSegredo();
        return new NotificacaoConfigResponse(
                c.getId(), c.getTenantId(), c.getCanal(), c.getProvider(), c.getRemetente(),
                c.getConfigJson(), temSegredo, temSegredo ? "********" : null,
                c.getLimiteDiario(), c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
