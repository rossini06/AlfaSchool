package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.TipoEnsino;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegraAprovacaoResponse(
        UUID id,
        UUID tenantId,
        TipoEnsino tipoEnsino,
        Boolean usaNotaNumerica,
        Boolean usaConceito,
        Boolean usaAvaliacaoDescritiva,
        BigDecimal notaMinimaAprovacao,
        BigDecimal frequenciaMinimaAprovacao,
        Boolean permiteRecuperacao,
        Boolean calculaMediaAritmetica,
        Boolean calculaMediaPonderada,
        Boolean exigeProjetoFinal,
        Boolean aprovacaoPorDisciplina,
        Boolean aprovacaoPorModulo,
        String conceitosPossiveis,
        String conceitoMinimoAprovacao,
        Instant createdAt,
        Instant updatedAt
) {
    public static RegraAprovacaoResponse from(RegraAprovacao r) {
        return new RegraAprovacaoResponse(
                r.getId(),
                r.getTenantId(),
                r.getTipoEnsino(),
                r.getUsaNotaNumerica(),
                r.getUsaConceito(),
                r.getUsaAvaliacaoDescritiva(),
                r.getNotaMinimaAprovacao(),
                r.getFrequenciaMinimaAprovacao(),
                r.getPermiteRecuperacao(),
                r.getCalculaMediaAritmetica(),
                r.getCalculaMediaPonderada(),
                r.getExigeProjetoFinal(),
                r.getAprovacaoPorDisciplina(),
                r.getAprovacaoPorModulo(),
                r.getConceitosPossiveis(),
                r.getConceitoMinimoAprovacao(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
