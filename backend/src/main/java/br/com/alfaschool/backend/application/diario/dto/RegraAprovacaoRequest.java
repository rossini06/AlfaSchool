package br.com.alfaschool.backend.application.diario.dto;

import br.com.alfaschool.backend.domain.diario.TipoEnsino;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegraAprovacaoRequest(
        @NotNull(message = "Tipo de ensino é obrigatório") TipoEnsino tipoEnsino,
        Boolean usaNotaNumerica,
        Boolean usaConceito,
        Boolean usaAvaliacaoDescritiva,
        BigDecimal notaMinimaAprovacao,
        BigDecimal frequenciaMinimaAprovacao,
        Boolean permiteRecuperacao,
        Boolean calculaMediaAritmetica,
        Boolean calculaMediaPonderada,
        Boolean exigeProjetoFinal,
        Boolean aprovacaoPorDisciplina,
        Boolean aprovacaoPorModulo,
        String conceitosPossiveis,
        String conceitoMinimoAprovacao
) {}
