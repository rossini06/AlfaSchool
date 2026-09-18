package br.com.alfaschool.backend.application.access.permanencia.dto;

import br.com.alfaschool.backend.domain.access.permanencia.AccPresencaPar;
import br.com.alfaschool.backend.domain.access.permanencia.OrigemPar;

import java.time.Instant;
import java.util.UUID;

public record PresencaParResponse(
        UUID id,
        Instant entradaEm,
        Instant saidaEm,
        Integer minutos,
        OrigemPar origem,
        UUID ajustadoPor,
        String motivoAjuste
) {
    public static PresencaParResponse from(AccPresencaPar p) {
        return new PresencaParResponse(p.getId(), p.getEntradaEm(), p.getSaidaEm(), p.getMinutos(),
                p.getOrigem(), p.getAjustadoPor(), p.getMotivoAjuste());
    }
}
