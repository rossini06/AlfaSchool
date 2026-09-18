package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoTemplate;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoTemplateResponse(
        UUID id,
        UUID tenantId,
        EventoNotificacao evento,
        CanalNotificacao canal,
        String assunto,
        String corpo,
        String templateExterno,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificacaoTemplateResponse from(AccNotificacaoTemplate t) {
        return new NotificacaoTemplateResponse(
                t.getId(), t.getTenantId(), t.getEvento(), t.getCanal(), t.getAssunto(), t.getCorpo(),
                t.getTemplateExterno(), t.isAtivo(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
