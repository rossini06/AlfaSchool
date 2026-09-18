package br.com.alfaschool.backend.application.access.permanencia.dto;

import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPresencaRepository;

import java.util.UUID;

/**
 * Totais de um aluno num periodo. Os dias INCONSISTENTES ja ficaram de
 * fora na consulta — por isso {@code dias} pode ser menor que o numero de
 * linhas do extrato, e isso e' correto, nao divergencia.
 */
public record TotaisAlunoResponse(
        UUID alunoId,
        long dias,
        long minutosPermanencia,
        long minutosPrevistos,
        long minutosExcedente,
        long minutosAntecipacao
) {
    public static TotaisAlunoResponse from(AccPresencaRepository.TotaisAluno t) {
        return new TotaisAlunoResponse(
                t.getAlunoId(),
                valor(t.getDias()),
                valor(t.getMinutosPermanencia()),
                valor(t.getMinutosPrevistos()),
                valor(t.getMinutosExcedente()),
                valor(t.getMinutosAntecipacao()));
    }

    private static long valor(Long v) {
        return v == null ? 0L : v;
    }
}
