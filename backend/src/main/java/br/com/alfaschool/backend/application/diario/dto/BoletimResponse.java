package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO para o boletim do aluno
 */
public record BoletimResponse(
        // Dados do aluno
        UUID alunoId,
        String alunoNome,
        String numeroMatricula,

        // Dados da turma
        UUID turmaId,
        String turmaNome,
        Integer anoLetivo,
        String turno,

        // Dados do curso
        UUID cursoId,
        String cursoNome,
        String nivel,

        // Período
        String periodo,

        // Dados resumidos
        Integer totalDisciplinas,
        BigDecimal mediaGeral,
        BigDecimal frequenciaGeral,
        SituacaoAluno situacaoGeral,

        // Disciplinas
        List<DisciplinaBoletim> disciplinas
) {
    public record DisciplinaBoletim(
            UUID disciplinaId,
            String disciplinaNome,
            String codigo,
            Integer cargaHoraria,
            BigDecimal media,
            BigDecimal percentualFrequencia,
            Integer totalAulas,
            Integer presencas,
            Integer faltas,
            Integer faltasJustificadas,
            SituacaoAluno situacao,
            String conceito,
            String observacao,
            List<AvaliacaoBoletim> avaliacoes
    ) {}

    public record AvaliacaoBoletim(
            UUID avaliacaoId,
            String nome,
            String tipo,
            BigDecimal peso,
            BigDecimal notaMaxima,
            BigDecimal nota,
            BigDecimal notaRecuperacao,
            BigDecimal notaFinal
    ) {}
}
