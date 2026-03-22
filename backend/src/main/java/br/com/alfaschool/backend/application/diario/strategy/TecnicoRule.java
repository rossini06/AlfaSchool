package br.com.alfaschool.backend.application.diario.strategy;

import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regras para Ensino Técnico.
 * - Usa nota numérica
 * - Pode ter módulos
 * - Aprovação por disciplina ou módulo
 * - Pode exigir projeto final
 * - Nota mínima geralmente mais alta (7.0)
 */
@Component
public class TecnicoRule implements EducationRuleStrategy {

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

        // Ensino técnico geralmente tem nota mínima maior
        BigDecimal notaMinima = regra.getNotaMinimaAprovacao() != null
                ? regra.getNotaMinimaAprovacao()
                : new BigDecimal("7.00");

        BigDecimal frequenciaMinima = regra.getFrequenciaMinimaAprovacao() != null
                ? regra.getFrequenciaMinimaAprovacao()
                : new BigDecimal("75.00");

        boolean aprovadoPorNota = media.compareTo(notaMinima) >= 0;
        boolean aprovadoPorFrequencia = percentualFrequencia.compareTo(frequenciaMinima) >= 0;

        // Verifica se exige projeto final
        if (regra.getExigeProjetoFinal() != null && regra.getExigeProjetoFinal()) {
            // Neste caso, a aprovação final depende também do projeto
            // que seria validado em outro momento
            if (!aprovadoPorNota || !aprovadoPorFrequencia) {
                if (!aprovadoPorFrequencia) {
                    return SituacaoAluno.REPROVADO_FREQUENCIA;
                }
                return SituacaoAluno.REPROVADO_NOTA;
            }
            // Se passou na nota e frequência, ainda precisa do projeto
            return SituacaoAluno.CURSANDO; // Aguardando projeto
        }

        if (aprovadoPorNota && aprovadoPorFrequencia) {
            return SituacaoAluno.APROVADO;
        }

        if (!aprovadoPorFrequencia) {
            return SituacaoAluno.REPROVADO_FREQUENCIA;
        }

        // Técnico permite recuperação se configurado
        if (regra.getPermiteRecuperacao() != null && regra.getPermiteRecuperacao()) {
            // Bug #6 fix: Garantir que limite de recuperação seja >= 0
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
        // Ensino técnico pode usar conceitos opcionalmente
        if (regra.getUsaConceito() != null && regra.getUsaConceito() && media != null) {
            String conceitos = regra.getConceitosPossiveis() != null
                    ? regra.getConceitosPossiveis()
                    : "A,B,C,D,E";
            String[] opcoes = conceitos.split(",");

            if (media.compareTo(new BigDecimal("9.0")) >= 0) {
                return opcoes.length > 0 ? opcoes[0].trim() : "A";
            } else if (media.compareTo(new BigDecimal("7.0")) >= 0) {
                return opcoes.length > 1 ? opcoes[1].trim() : "B";
            } else if (media.compareTo(new BigDecimal("5.0")) >= 0) {
                return opcoes.length > 2 ? opcoes[2].trim() : "C";
            } else if (media.compareTo(new BigDecimal("3.0")) >= 0) {
                return opcoes.length > 3 ? opcoes[3].trim() : "D";
            } else {
                return opcoes.length > 4 ? opcoes[4].trim() : "E";
            }
        }
        return null;
    }

    @Override
    public boolean usaNotaNumerica(RegraAprovacao regra) {
        return true;
    }
}
