package br.com.alfaschool.backend.application.access.jornada.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Matricular varios alunos no mesmo plano de uma vez — a operacao real e'
 * "a turma toda passa para o plano de 5h".
 *
 * {@code encerrarVinculoAnterior} existe porque o caso comum e' TROCA de
 * plano, nao primeiro vinculo: sem ele a operacao bateria na validacao de
 * sobreposicao aluno por aluno. Encerrando o vinculo antigo na vespera da
 * nova vigencia, o passado continua apurado pela jornada antiga.
 */
public record AplicarJornadaRequest(
        @NotNull UUID jornadaId,
        @NotEmpty List<UUID> alunoIds,
        @NotNull LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        @Size(max = 255) String observacao,
        boolean encerrarVinculoAnterior
) {
}
