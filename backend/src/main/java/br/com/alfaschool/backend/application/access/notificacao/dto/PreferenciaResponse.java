package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoPreferencia;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;

import java.time.Instant;
import java.util.UUID;

public record PreferenciaResponse(
        UUID id,
        UUID tenantId,
        TitularTipo titularTipo,
        UUID titularId,
        CanalNotificacao canal,
        EventoNotificacao evento,
        String destino,
        boolean habilitado,
        Instant optInEm,
        Instant optOutEm,
        boolean podeReceber
) {
    public static PreferenciaResponse from(AccNotificacaoPreferencia p) {
        return new PreferenciaResponse(
                p.getId(), p.getTenantId(), p.getTitularTipo(), p.getTitularId(), p.getCanal(), p.getEvento(),
                p.getDestino(), p.isHabilitado(), p.getOptInEm(), p.getOptOutEm(), p.temConsentimento());
    }
}
