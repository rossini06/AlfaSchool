package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.jornada.AccAlunoJornada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoJornadaResponse(
        UUID id,
        UUID alunoId,
        String alunoNome,
        UUID jornadaId,
        String jornadaNome,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        String observacao,
        Instant createdAt,
        Instant updatedAt
) {
    public static AlunoJornadaResponse from(AccAlunoJornada v) {
        return from(v, null, null);
    }

    /**
     * A tela lista alunos e jornadas diferentes em cada linha. Sem os nomes
     * resolvidos aqui, as duas colunas principais mostrariam UUID.
     */
    public static AlunoJornadaResponse from(AccAlunoJornada v, String alunoNome, String jornadaNome) {
        return new AlunoJornadaResponse(v.getId(), v.getAlunoId(), alunoNome,
                v.getJornadaId(), jornadaNome,
                v.getVigenciaInicio(), v.getVigenciaFim(), v.getObservacao(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
