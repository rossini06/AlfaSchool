package br.com.alfaschool.backend.application.access.permanencia.dto;

import br.com.alfaschool.backend.domain.access.permanencia.AccPresenca;
import br.com.alfaschool.backend.domain.access.permanencia.AccPresencaPar;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Um dia do aluno. {@code contaNosTotais} vem explicito para que a tela
 * consiga rotular a linha inconsistente em vez de o operador descobrir a
 * divergencia somando a mao e achando que o sistema errou.
 */
public record PresencaResponse(
        UUID id,
        UUID alunoId,
        UUID unitId,
        LocalDate data,
        Instant primeiraEntradaEm,
        Instant ultimaSaidaEm,
        int minutosPermanencia,
        int minutosPrevistos,
        int minutosExcedente,
        int minutosAntecipacao,
        UUID jornadaId,
        boolean diaLetivo,
        StatusPresenca status,
        boolean congelada,
        Instant congeladaEm,
        boolean contaNosTotais,
        String observacao,
        List<PresencaParResponse> pares
) {
    public static PresencaResponse from(AccPresenca p, List<AccPresencaPar> pares) {
        List<PresencaParResponse> linhas = pares == null ? List.of()
                : pares.stream().map(PresencaParResponse::from).toList();
        return new PresencaResponse(p.getId(), p.getAlunoId(), p.getUnitId(), p.getData(),
                p.getPrimeiraEntradaEm(), p.getUltimaSaidaEm(), p.getMinutosPermanencia(),
                p.getMinutosPrevistos(), p.getMinutosExcedente(), p.getMinutosAntecipacao(),
                p.getJornadaId(), p.isDiaLetivo(), p.getStatus(), p.isCongelada(), p.getCongeladaEm(),
                p.getStatus() != StatusPresenca.INCONSISTENTE, p.getObservacao(), linhas);
    }

    public static PresencaResponse from(AccPresenca p) {
        return from(p, List.of());
    }
}
