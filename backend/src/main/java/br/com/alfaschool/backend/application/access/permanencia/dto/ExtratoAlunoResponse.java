package br.com.alfaschool.backend.application.access.permanencia.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Extrato diario do aluno. {@code diasInconsistentes} vem separado de
 * proposito: e' a lista de pendencias que alguem precisa corrigir antes
 * do fechamento, e o total nao a inclui.
 */
public record ExtratoAlunoResponse(
        UUID alunoId,
        LocalDate inicio,
        LocalDate fim,
        List<PresencaResponse> dias,
        int diasInconsistentes,
        TotaisAlunoResponse totais
) {
}
