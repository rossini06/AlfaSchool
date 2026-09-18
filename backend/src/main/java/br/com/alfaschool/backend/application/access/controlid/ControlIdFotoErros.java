package br.com.alfaschool.backend.application.access.controlid;

import java.util.Locale;
import java.util.Map;

/**
 * Traducao dos codigos de recusa de cadastro facial do Control iD.
 *
 * Tabela oficial:
 * https://www.controlid.com.br/docs/access-api-pt/reconhecimento-facial/cadastro-facial/
 *
 * Duas fontes de verdade, nessa ordem:
 *  1. errors[0].code — numerico, presente nos firmwares atuais;
 *  2. errors[0].message — texto em ingles, unico sinal nos firmwares
 *     antigos, que respondem sem code.
 *
 * Quando nada casa, a mensagem ORIGINAL do equipamento e' devolvida.
 * Esconder o motivo atras de "erro ao cadastrar" foi exatamente o bug
 * que deixava a secretaria sem acao possivel.
 */
public final class ControlIdFotoErros {

    private ControlIdFotoErros() {
    }

    public static final String CODIGO_DESCONHECIDO = "DESCONHECIDO";

    private static final Map<Integer, String> POR_CODIGO = Map.ofEntries(
            Map.entry(1, "Arquivo de foto inválido — envie JPG ou PNG."),
            Map.entry(2, "Nenhum rosto detectado na foto."),
            Map.entry(3, "Este rosto já está cadastrado para outra pessoa."),
            Map.entry(4, "Rosto descentralizado na foto."),
            Map.entry(5, "Rosto muito pequeno/distante na foto."),
            Map.entry(6, "Rosto muito próximo da câmera."),
            Map.entry(7, "Cabeça inclinada na foto."),
            Map.entry(8, "Foto sem nitidez (borrada) ou de baixa qualidade."),
            Map.entry(9, "Rosto cortado na borda da foto.")
    );

    /**
     * Fallback por texto. A ORDEM IMPORTA: "too close to image borders"
     * precisa ser testado antes de "too close", senao rosto cortado vira
     * "rosto muito proximo" e o operador aproxima ainda mais a camera.
     */
    private static final String[][] POR_MENSAGEM = {
            {"face not detected", "2"},
            {"no face", "2"},
            {"face exists", "3"},
            {"already exists", "3"},
            {"too close to image borders", "9"},
            {"too distant", "5"},
            {"too small", "5"},
            {"too close", "6"},
            {"low sharpness", "8"},
            {"blurred", "8"},
            {"closed eyes", "8"},
            {"eyes closed", "8"},
            {"pose", "7"},
            {"not centered", "4"}
    };

    /**
     * @param code    errors[0].code, ou null quando o firmware nao manda
     * @param message errors[0].message, texto cru do equipamento
     */
    public static Recusa traduzir(Integer code, String message) {
        if (code != null) {
            String texto = POR_CODIGO.get(code);
            if (texto != null) {
                return new Recusa(String.valueOf(code), texto);
            }
        }
        if (message != null && !message.isBlank()) {
            String normalizada = message.toLowerCase(Locale.ROOT);
            // "not centered" + "pose" juntos significam cabeca inclinada,
            // nao apenas descentralizada: testa o par antes das partes.
            if (normalizada.contains("not centered") && normalizada.contains("pose")) {
                return new Recusa("7", POR_CODIGO.get(7));
            }
            for (String[] par : POR_MENSAGEM) {
                if (normalizada.contains(par[0])) {
                    int c = Integer.parseInt(par[1]);
                    return new Recusa(String.valueOf(c), POR_CODIGO.get(c));
                }
            }
            // Nunca esconde o motivo: repassa o texto do equipamento.
            return new Recusa(code == null ? CODIGO_DESCONHECIDO : String.valueOf(code), message);
        }
        return new Recusa(code == null ? CODIGO_DESCONHECIDO : String.valueOf(code),
                "O equipamento recusou a foto sem informar o motivo.");
    }

    public record Recusa(String codigo, String mensagem) {
    }
}
