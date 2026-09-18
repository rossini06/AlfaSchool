package br.com.alfaschool.backend.application.access.notificacao.sender;

/**
 * Resultado de uma tentativa de entrega.
 *
 * <p>A distincao entre erro PERMANENTE e TRANSITORIO e' o coracao da fila.
 * Permanente (endereco invalido, opt-out no provedor, numero inexistente,
 * template nao aprovado) nao melhora com repeticao: insistir so' queima cota e
 * reputacao do remetente. Transitorio (timeout, 5xx, rate limit) melhora:
 * volta para a fila com backoff.
 *
 * @param sucesso           entrega aceita pelo provedor.
 * @param providerMessageId id devolvido pelo provedor, para rastreio.
 * @param codigoErro        codigo curto para agrupar falhas no historico.
 * @param mensagemErro      texto para o operador entender o que houve.
 * @param permanente        true = sai da fila; false = reagenda.
 */
public record ResultadoEnvio(
        boolean sucesso,
        String providerMessageId,
        String codigoErro,
        String mensagemErro,
        boolean permanente
) {

    public static ResultadoEnvio ok(String providerMessageId) {
        return new ResultadoEnvio(true, providerMessageId, null, null, false);
    }

    public static ResultadoEnvio permanente(String codigo, String mensagem) {
        return new ResultadoEnvio(false, null, codigo, truncar(mensagem), true);
    }

    public static ResultadoEnvio transitorio(String codigo, String mensagem) {
        return new ResultadoEnvio(false, null, codigo, truncar(mensagem), false);
    }

    private static String truncar(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 480 ? s : s.substring(0, 480);
    }
}
