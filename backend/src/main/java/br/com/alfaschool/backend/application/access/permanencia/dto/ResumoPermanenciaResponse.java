package br.com.alfaschool.backend.application.access.permanencia.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Totalizacao do periodo por aluno e, quando ha filtro de turma, o
 * consolidado dela. Dias INCONSISTENTES ficaram fora de ambos.
 */
public record ResumoPermanenciaResponse(
        LocalDate inicio,
        LocalDate fim,
        UUID turmaId,
        UUID unitId,
        List<TotaisAlunoResponse> porAluno,
        TotaisTurma turma
) {
    public record TotaisTurma(
            int alunos,
            long dias,
            long minutosPermanencia,
            long minutosPrevistos,
            long minutosExcedente,
            long minutosAntecipacao
    ) {
        public static TotaisTurma de(List<TotaisAlunoResponse> linhas) {
            return new TotaisTurma(
                    linhas.size(),
                    linhas.stream().mapToLong(TotaisAlunoResponse::dias).sum(),
                    linhas.stream().mapToLong(TotaisAlunoResponse::minutosPermanencia).sum(),
                    linhas.stream().mapToLong(TotaisAlunoResponse::minutosPrevistos).sum(),
                    linhas.stream().mapToLong(TotaisAlunoResponse::minutosExcedente).sum(),
                    linhas.stream().mapToLong(TotaisAlunoResponse::minutosAntecipacao).sum());
        }
    }
}
