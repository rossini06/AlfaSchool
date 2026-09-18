package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;

import java.time.Instant;
import java.util.UUID;

public record AutorizacaoHistoricoResponse(
        UUID id,
        UUID autorizacaoId,
        AcaoAutorizacao acao,
        StatusAutorizacao statusAnterior,
        StatusAutorizacao statusNovo,
        UUID userId,
        String motivo,
        String ip,
        Instant createdAt
) {
    public static AutorizacaoHistoricoResponse from(AutorizacaoHistorico h) {
        return new AutorizacaoHistoricoResponse(
                h.getId(), h.getAutorizacaoId(), h.getAcao(),
                h.getStatusAnterior(), h.getStatusNovo(),
                h.getUserId(), h.getMotivo(), h.getIp(), h.getCreatedAt()
        );
    }
}
