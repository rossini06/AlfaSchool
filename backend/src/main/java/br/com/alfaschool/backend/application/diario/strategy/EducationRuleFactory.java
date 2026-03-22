package br.com.alfaschool.backend.application.diario.strategy;

import br.com.alfaschool.backend.domain.diario.TipoEnsino;
import org.springframework.stereotype.Component;

/**
 * Factory para obter a estratégia correta baseada no tipo de ensino.
 */
@Component
public class EducationRuleFactory {

    private final RegularEducationRule regularEducationRule;
    private final InfantilRule infantilRule;
    private final TecnicoRule tecnicoRule;

    public EducationRuleFactory(
            RegularEducationRule regularEducationRule,
            InfantilRule infantilRule,
            TecnicoRule tecnicoRule) {
        this.regularEducationRule = regularEducationRule;
        this.infantilRule = infantilRule;
        this.tecnicoRule = tecnicoRule;
    }

    /**
     * Retorna a estratégia apropriada para o tipo de ensino.
     *
     * @param tipoEnsino O tipo de ensino
     * @return A estratégia correspondente
     */
    public EducationRuleStrategy getStrategy(TipoEnsino tipoEnsino) {
        if (tipoEnsino == null) {
            return regularEducationRule;
        }

        return switch (tipoEnsino) {
            case INFANTIL -> infantilRule;
            case FUNDAMENTAL, MEDIO -> regularEducationRule;
            case TECNICO -> tecnicoRule;
        };
    }

    /**
     * Retorna a estratégia baseada no nível do curso (string).
     *
     * @param nivel O nível do curso como string
     * @return A estratégia correspondente
     */
    public EducationRuleStrategy getStrategyByNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            return regularEducationRule;
        }

        String nivelLower = nivel.toLowerCase().trim();

        if (nivelLower.contains("infantil") || nivelLower.contains("educação infantil")) {
            return infantilRule;
        } else if (nivelLower.contains("técnico") || nivelLower.contains("tecnico") ||
                   nivelLower.contains("profissional") || nivelLower.contains("profissionalizante")) {
            return tecnicoRule;
        } else {
            // Fundamental, Médio, ou qualquer outro
            return regularEducationRule;
        }
    }
}
