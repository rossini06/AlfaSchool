package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.retirada.AccOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.StatusOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaResponse(
        UUID id,
        UUID unitId,
        TipoOcorrencia tipo,
        GravidadeOcorrencia gravidade,
        StatusOcorrencia status,
        UUID alunoId,
        UUID pessoaAutorizadaId,
        UUID dispositivoId,
        UUID eventoId,
        UUID retiradaId,
        String descricao,
        UUID tratadoPorUserId,
        Instant tratadoEm,
        String tratativa,
        Instant createdAt
) {
    public static OcorrenciaResponse from(AccOcorrencia o) {
        return new OcorrenciaResponse(
                o.getId(),
                o.getUnitId(),
                o.getTipo(),
                o.getGravidade(),
                o.getStatus(),
                o.getAlunoId(),
                o.getPessoaAutorizadaId(),
                o.getDispositivoId(),
                o.getEventoId(),
                o.getRetiradaId(),
                o.getDescricao(),
                o.getTratadoPorUserId(),
                o.getTratadoEm(),
                o.getTratativa(),
                o.getCreatedAt());
    }
}
