package br.com.alfaschool.backend.domain.access.retirada;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Unico lugar que decide se uma transicao de retirada e' legitima.
 *
 * Existe para que a regra nao se espalhe em ifs por services e controllers:
 * quando a escola pedir um estado novo, muda-se o mapa aqui e nada mais.
 *
 * Fluxo feliz: SOLICITADA -> PREPARANDO -> PRONTO -> ENTREGUE.
 * De qualquer estado nao-final da' para CANCELAR ou NEGAR.
 * ENTREGUE, CANCELADA e NEGADA sao finais: nao voltam atras. Desfazer uma
 * entrega seria reescrever um ato de responsabilidade ja praticado — se a
 * escola errou, abre-se uma ocorrencia, nao se apaga o historico.
 */
public final class MaquinaEstadosRetirada {

    private static final Map<StatusRetirada, Set<StatusRetirada>> PERMITIDAS =
            new EnumMap<>(StatusRetirada.class);

    static {
        PERMITIDAS.put(StatusRetirada.SOLICITADA, EnumSet.of(
                StatusRetirada.PREPARANDO,
                // A escola pequena entrega direto da portaria, sem passar
                // pela sala: o salto para PRONTO e ENTREGUE e' legitimo.
                StatusRetirada.PRONTO,
                StatusRetirada.ENTREGUE,
                StatusRetirada.CANCELADA,
                StatusRetirada.NEGADA));
        PERMITIDAS.put(StatusRetirada.PREPARANDO, EnumSet.of(
                StatusRetirada.PRONTO,
                StatusRetirada.ENTREGUE,
                StatusRetirada.CANCELADA,
                StatusRetirada.NEGADA));
        PERMITIDAS.put(StatusRetirada.PRONTO, EnumSet.of(
                StatusRetirada.ENTREGUE,
                StatusRetirada.CANCELADA,
                StatusRetirada.NEGADA));
        PERMITIDAS.put(StatusRetirada.ENTREGUE, EnumSet.noneOf(StatusRetirada.class));
        PERMITIDAS.put(StatusRetirada.CANCELADA, EnumSet.noneOf(StatusRetirada.class));
        PERMITIDAS.put(StatusRetirada.NEGADA, EnumSet.noneOf(StatusRetirada.class));
    }

    private MaquinaEstadosRetirada() {
    }

    public static boolean ehFinal(StatusRetirada status) {
        return status == StatusRetirada.ENTREGUE
                || status == StatusRetirada.CANCELADA
                || status == StatusRetirada.NEGADA;
    }

    public static boolean permitida(StatusRetirada de, StatusRetirada para) {
        if (de == null || para == null) {
            return false;
        }
        return PERMITIDAS.getOrDefault(de, EnumSet.noneOf(StatusRetirada.class)).contains(para);
    }

    /**
     * Devolve a mensagem de 409 quando a transicao e' invalida, ou null
     * quando e' valida. A mensagem e' de operacao, nao de programador: quem
     * le esta' na portaria com o pai na frente.
     */
    public static String validarTransicao(StatusRetirada de, StatusRetirada para) {
        if (de == null || para == null) {
            return "Transicao de retirada incompleta";
        }
        if (de == para) {
            return "Retirada ja esta " + rotulo(de);
        }
        if (permitida(de, para)) {
            return null;
        }
        if (ehFinal(de)) {
            return "Retirada ja " + rotulo(de);
        }
        return "Nao e possivel mudar a retirada de " + rotulo(de) + " para " + rotulo(para);
    }

    private static String rotulo(StatusRetirada status) {
        return switch (status) {
            case SOLICITADA -> "solicitada";
            case PREPARANDO -> "em preparo";
            case PRONTO -> "pronta";
            case ENTREGUE -> "entregue";
            case CANCELADA -> "cancelada";
            case NEGADA -> "negada";
        };
    }
}
