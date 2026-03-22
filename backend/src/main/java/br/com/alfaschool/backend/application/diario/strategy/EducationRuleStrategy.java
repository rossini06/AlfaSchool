package br.com.alfaschool.backend.application.diario.strategy;

import br.com.alfaschool.backend.domain.diario.Media;
import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import br.com.alfaschool.backend.domain.nota.Nota;
import java.math.BigDecimal;
import java.util.List;

/**
 * Interface Strategy para regras de aprovação por tipo de ensino.
 * Cada tipo de ensino (Infantil, Fundamental, Médio, Técnico) tem suas próprias regras.
 */
public interface EducationRuleStrategy {

    /**
     * Calcula a média do aluno baseado nas notas e pesos das avaliações.
     *
     * @param notas Lista de notas do aluno
     * @param avaliacoesComPeso Mapa de avaliacaoId -> peso
     * @return Média calculada ou null se não aplicável
     */
    BigDecimal calcularMedia(List<NotaComPeso> notas);

    /**
     * Determina a situação do aluno baseado na média e frequência.
     *
     * @param media A média do aluno (pode ser null para ensino infantil)
     * @param percentualFrequencia O percentual de frequência
     * @param regra As regras de aprovação configuradas
     * @return A situação do aluno
     */
    SituacaoAluno determinarSituacao(BigDecimal media, BigDecimal percentualFrequencia, RegraAprovacao regra);

    /**
     * Converte nota numérica para conceito (para ensino infantil).
     *
     * @param media A média numérica
     * @param regra As regras de aprovação
     * @return O conceito correspondente ou null
     */
    String converterParaConceito(BigDecimal media, RegraAprovacao regra);

    /**
     * Verifica se a avaliação é válida para este tipo de ensino.
     *
     * @param regra As regras de aprovação
     * @return true se usa nota numérica
     */
    boolean usaNotaNumerica(RegraAprovacao regra);

    /**
     * Classe auxiliar para transportar nota com peso
     */
    record NotaComPeso(
            Nota nota,
            BigDecimal peso,
            BigDecimal notaMaxima
    ) {}
}
