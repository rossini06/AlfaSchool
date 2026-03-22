package br.com.alfaschool.backend.application.diario.strategy;

import br.com.alfaschool.backend.domain.diario.RegraAprovacao;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regras para Ensino Infantil.
 * - NÃO usa nota numérica
 * - Usa conceitos (A, B, C)
 * - Avaliação descritiva
 * - Frequência obrigatória mas menos rígida
 */
@Component
public class InfantilRule implements EducationRuleStrategy {

    @Override
    public BigDecimal calcularMedia(List<NotaComPeso> notas) {
        // Ensino infantil não calcula média numérica
        return null;
    }

    @Override
    public SituacaoAluno determinarSituacao(BigDecimal media, BigDecimal percentualFrequencia, RegraAprovacao regra) {
        if (percentualFrequencia == null) {
            return SituacaoAluno.CURSANDO;
        }

        BigDecimal frequenciaMinima = regra.getFrequenciaMinimaAprovacao() != null
                ? regra.getFrequenciaMinimaAprovacao()
                : new BigDecimal("75.00");

        // No ensino infantil, a aprovação é principalmente por frequência
        // e participação (avaliação descritiva)
        if (percentualFrequencia.compareTo(frequenciaMinima) >= 0) {
            return SituacaoAluno.APROVADO;
        }

        return SituacaoAluno.REPROVADO_FREQUENCIA;
    }

    @Override
    public String converterParaConceito(BigDecimal media, RegraAprovacao regra) {
        // No infantil, o conceito é atribuído manualmente pelo professor
        // baseado na avaliação descritiva. Este método retorna um conceito
        // padrão se não houver avaliação específica
        if (media == null) {
            return null;
        }

        String conceitos = regra.getConceitosPossiveis() != null
                ? regra.getConceitosPossiveis()
                : "A,B,C";
        String[] opcoes = conceitos.split(",");

        // Mapeia nota (0-10) para conceito
        if (media.compareTo(new BigDecimal("8.0")) >= 0) {
            return opcoes.length > 0 ? opcoes[0].trim() : "A"; // Ótimo
        } else if (media.compareTo(new BigDecimal("6.0")) >= 0) {
            return opcoes.length > 1 ? opcoes[1].trim() : "B"; // Bom
        } else {
            return opcoes.length > 2 ? opcoes[2].trim() : "C"; // Regular
        }
    }

    @Override
    public boolean usaNotaNumerica(RegraAprovacao regra) {
        return false;
    }
}
