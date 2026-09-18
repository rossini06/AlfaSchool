package br.com.alfaschool.backend.application.access.controlid;

import java.util.regex.Pattern;

/**
 * Whitelist de acionamento do equipamento (abrir porta, liberar catraca).
 *
 * O firmware aceita {"actions":[{"action":"X","parameters":"Y"}]} com
 * Y sendo uma STRING livre. Encaminhar para la' o que chega da API seria
 * entregar o painel de comandos do leitor para quem faz a requisicao.
 * Aqui so' existem os quatro acionamentos abaixo, e cada um valida os
 * proprios parametros por regex antes de virar payload.
 *
 * Armadilha de firmware herdada dos projetos anteriores: a chave e'
 * "door" (numero do portal). Mandar "id=1" devolve HTTP 200 e NAO pulsa
 * o rele — sucesso falso, porta fechada, aluno parado na portaria.
 *
 * Acionamento NAO e' idempotente: nunca repita a chamada as cegas depois
 * de um timeout, ou a catraca libera duas passagens.
 */
public record AcionamentoAcesso(Tipo tipo, String parametros) {

    public enum Tipo {
        /** Abre porta/portal 1 ou 2. */
        PORTA("door", Pattern.compile("^door=[12]$")),
        /** Caixa de seguranca. O espaco depois da virgula e' exigido pelo firmware. */
        SEC_BOX("sec_box", Pattern.compile("^id=[1-9]\\d*, reason=3$")),
        /** Sentido de giro liberado na catraca. */
        CATRACA_GIRO("catra", Pattern.compile("^allow=(clockwise|anticlockwise|both)$")),
        /** Pulso direto de rele da catraca. */
        CATRACA_RELE("catra", Pattern.compile("^relay=[12]$"));

        private final String action;
        private final Pattern regex;

        Tipo(String action, Pattern regex) {
            this.action = action;
            this.regex = regex;
        }

        public String action() {
            return action;
        }

        public boolean aceita(String parametros) {
            return parametros != null && regex.matcher(parametros).matches();
        }
    }

    public AcionamentoAcesso {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de acionamento obrigatório.");
        }
        if (!tipo.aceita(parametros)) {
            throw new IllegalArgumentException(
                    "Parâmetros de acionamento inválidos para " + tipo + ": " + parametros);
        }
    }

    public String action() {
        return tipo.action();
    }

    public static AcionamentoAcesso abrirPorta(int porta) {
        return new AcionamentoAcesso(Tipo.PORTA, "door=" + porta);
    }

    public static AcionamentoAcesso liberarCatraca(String sentido) {
        return new AcionamentoAcesso(Tipo.CATRACA_GIRO, "allow=" + sentido);
    }

    public static AcionamentoAcesso pulsarRele(int rele) {
        return new AcionamentoAcesso(Tipo.CATRACA_RELE, "relay=" + rele);
    }

    /**
     * Constroi a partir do que chega da API. Tipo desconhecido e'
     * rejeitado aqui, nao no equipamento.
     */
    public static AcionamentoAcesso de(String tipo, String parametros) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo de acionamento obrigatório.");
        }
        Tipo t;
        try {
            t = Tipo.valueOf(tipo.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de acionamento não permitido: " + tipo);
        }
        return new AcionamentoAcesso(t, parametros);
    }
}
