package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.retirada.AccRetiradaHistorico;
import br.com.alfaschool.backend.domain.access.retirada.OrigemTransicao;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.time.Instant;
import java.util.UUID;

public record RetiradaHistoricoResponse(
        UUID id,
        StatusRetirada statusAnterior,
        StatusRetirada statusNovo,
        UUID userId,
        OrigemTransicao origem,
        String motivo,
        String ip,
        Instant createdAt
) {
    public static RetiradaHistoricoResponse from(AccRetiradaHistorico h) {
        return new RetiradaHistoricoResponse(
                h.getId(),
                h.getStatusAnterior(),
                h.getStatusNovo(),
                h.getUserId(),
                h.getOrigem(),
                h.getMotivo(),
                h.getIp(),
                h.getCreatedAt());
    }
}
