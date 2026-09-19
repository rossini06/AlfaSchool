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
        Instant ocorridoEm,
        UUID alunoId,
        String alunoNome,
        UUID pessoaAutorizadaId,
        UUID dispositivoId,
        UUID eventoId,
        UUID retiradaId,
        String descricao,
        UUID tratadoPorUserId,
        String tratadoPorNome,
        Instant tratadoEm,
        String tratativa,
        Instant createdAt
) {
    public static OcorrenciaResponse from(AccOcorrencia o) {
        return from(o, null, null);
    }

    /**
     * A tela lista ocorrencias de alunos diferentes, tratadas por pessoas
     * diferentes. Sem o nome resolvido aqui ela precisaria de uma consulta
     * por linha — ou, como estava, deixaria as colunas "Aluno" e "Tratada
     * por" permanentemente vazias.
     */
    public static OcorrenciaResponse from(AccOcorrencia o, String alunoNome, String tratadoPorNome) {
        return new OcorrenciaResponse(
                o.getId(),
                o.getUnitId(),
                o.getTipo(),
                o.getGravidade(),
                o.getStatus(),
                // Ocorrencia antiga, gravada antes de existir a coluna, cai
                // no instante do registro — que era o unico que havia.
                o.getOcorridoEm() != null ? o.getOcorridoEm() : o.getCreatedAt(),
                o.getAlunoId(),
                alunoNome,
                o.getPessoaAutorizadaId(),
                o.getDispositivoId(),
                o.getEventoId(),
                o.getRetiradaId(),
                o.getDescricao(),
                o.getTratadoPorUserId(),
                tratadoPorNome,
                o.getTratadoEm(),
                o.getTratativa(),
                o.getCreatedAt());
    }
}
