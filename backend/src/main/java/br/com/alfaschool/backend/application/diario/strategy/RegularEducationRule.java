package br.com.alfaschool.backend.application.diario.strategy;

import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regras para Ensino Fundamental e Médio.
 * - Usa nota numérica
 * - Média aritmética ou ponderada
 * - Frequência obrigatória
 * - Permite recuperação
 */
@Component
public class RegularEducationRule implements EducationRuleStrategy {

    @Override
    public BigDecimal calcularMedia(List<NotaComPeso> notas) {
        if (notas == null || notas.isEmpty()) {
            return null;
        }

        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesos = BigDecimal.ZERO;

        for (NotaComPeso item : notas) {
            // Validação de null safety
            if (item == null || item.nota() == null || item.nota().getNotaFinal() == null || item.peso() == null) {
                continue;
            }

            // Normaliza a nota para escala de 0-10 se necessário
            BigDecimal notaNormalizada = item.nota().getNotaFinal();

            // Bug #4 fix: Validar que notaMaxima > 0 antes de dividir
            if (item.notaMaxima() != null
                    && item.notaMaxima().compareTo(BigDecimal.ZERO) > 0
                    && item.notaMaxima().compareTo(BigDecimal.TEN) != 0) {
                notaNormalizada = notaNormalizada
                        .multiply(BigDecimal.TEN)
                        .divide(item.notaMaxima(), 2, RoundingMode.HALF_UP);
            }

            somaPonderada = somaPonderada.add(notaNormalizada.multiply(item.peso()));
            somaPesos = somaPesos.add(item.peso());
        }

        if (somaPesos.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return somaPonderada.divide(somaPesos, 2, RoundingMode.HALF_UP);
    }

    @Override
    public SituacaoAluno determinarSituacao(BigDecimal media, BigDecimal percentualFrequencia, RegraAprovacao regra) {
        if (media == null || percentualFrequencia == null) {
            return SituacaoAluno.CURSANDO;
        }

        BigDecimal notaMinima = regra.getNotaMinimaAprovacao() != null
                ? regra.getNotaMinimaAprovacao()
                : new BigDecimal("6.00");

        BigDecimal frequenciaMinima = regra.getFrequenciaMinimaAprovacao() != null
                ? regra.getFrequenciaMinimaAprovacao()
                : new BigDecimal("75.00");

        boolean aprovadoPorNota = media.compareTo(notaMinima) >= 0;
        boolean aprovadoPorFrequencia = percentualFrequencia.compareTo(frequenciaMinima) >= 0;

        if (aprovadoPorNota && aprovadoPorFrequencia) {
            return SituacaoAluno.APROVADO;
        }

        if (!aprovadoPorFrequencia) {
            return SituacaoAluno.REPROVADO_FREQUENCIA;
        }

        // Verifica se pode ir para recuperação
        if (regra.getPermiteRecuperacao() != null && regra.getPermiteRecuperacao()) {
            // Bug #6 fix: Garantir que limite de recuperação seja >= 0
            // Se nota mínima for baixa, usar limite mínimo de 0
            BigDecimal limiteRecuperacao = notaMinima.subtract(new BigDecimal("2.00"));
            if (limiteRecuperacao.compareTo(BigDecimal.ZERO) < 0) {
                limiteRecuperacao = BigDecimal.ZERO;
            }
            if (media.compareTo(limiteRecuperacao) >= 0) {
                return SituacaoAluno.RECUPERACAO;
            }
        }

        return SituacaoAluno.REPROVADO_NOTA;
    }

    @Override
    public String converterParaConceito(BigDecimal media, RegraAprovacao regra) {
        // Ensino regular não usa conceitos por padrão
        return null;
    }

    @Override
    public boolean usaNotaNumerica(RegraAprovacao regra) {
        return true;
    }
}
