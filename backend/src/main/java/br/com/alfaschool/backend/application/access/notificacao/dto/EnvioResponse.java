package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha do historico. Nada do provedor aparece aqui alem do
 * {@code providerMessageId}: nenhum token, nenhum endpoint autenticado.
 */
public record EnvioResponse(
        UUID id,
        UUID tenantId,
        CanalNotificacao canal,
        EventoNotificacao evento,
        TitularTipo titularTipo,
        UUID titularId,
        UUID alunoId,
        String destino,
        String assunto,
        String corpo,
        StatusEnvio status,
        Integer tentativas,
        String erro,
        boolean erroPermanente,
        String providerMessageId,
        String chaveIdempotencia,
        Instant agendadoPara,
        Instant enviadoEm,
        Instant entregueEm,
        Instant createdAt
) {
    public static EnvioResponse from(AccNotificacaoEnvio e) {
        return new EnvioResponse(
                e.getId(), e.getTenantId(), e.getCanal(), e.getEvento(), e.getTitularTipo(), e.getTitularId(),
                e.getAlunoId(), e.getDestino(), e.getAssunto(), e.getCorpo(), e.getStatus(), e.getTentativas(),
                e.getErro(), e.erroPermanente(), e.getProviderMessageId(), e.getChaveIdempotencia(),
                e.getAgendadoPara(), e.getEnviadoEm(), e.getEntregueEm(), e.getCreatedAt());
    }
}
