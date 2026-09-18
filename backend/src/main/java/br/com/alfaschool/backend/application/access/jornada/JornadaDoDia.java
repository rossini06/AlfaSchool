package br.com.alfaschool.backend.application.access.jornada;

import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;

import java.time.LocalTime;
import java.util.UUID;

/**
 * O contratado de UM aluno em UM dia, com a excecao pontual ja aplicada.
 *
 * E' o contrato entre a fatia de jornadas e o motor de permanencia: o
 * motor nao sabe (nem precisa saber) se o numero veio da jornada vigente
 * ou da excecao daquele dia — ele so' apura.
 *
 * @param jornadaId jornada de onde vieram tolerancia e regra. Nulo quando
 *                  o aluno nao tem vinculo vigente e o dia so' existe por
 *                  causa de uma excecao pontual.
 * @param origemExcecao true quando uma excecao sobrescreveu o dia — vai
 *                      para o log de apuracao, porque explica sozinho a
 *                      metade das duvidas da secretaria.
 */
public record JornadaDoDia(
        UUID jornadaId,
        boolean frequenta,
        LocalTime entradaPrevista,
        LocalTime saidaPrevista,
        int cargaMinutos,
        int toleranciaEntradaMin,
        int toleranciaSaidaMin,
        RegraExcedente regraExcedente,
        boolean origemExcecao
) {
    /** Aluno sem jornada contratada: nao ha previsto, logo nao ha o que descontar. */
    public static JornadaDoDia semContrato() {
        return new JornadaDoDia(null, false, null, null, 0, 0, 0, RegraExcedente.HORARIO, false);
    }
}
