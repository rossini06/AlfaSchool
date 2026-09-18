package br.com.alfaschool.backend.application.access.notificacao.sender;

/**
 * Camada fina de transporte de e-mail.
 *
 * <p>Duas implementacoes convivem: {@link SmtpEmailTransport}, que conecta de
 * verdade e entra quando o tenant tem SMTP configurado, e
 * {@link LogEmailTransport}, que apenas registra e e' o default em
 * laboratorio. A escolha e' por CONTA: sem conta SMTP valida, cai no log.
 *
 * <p>Isso importa porque "entregou no log" e "entregou na caixa da familia"
 * nao podem parecer a mesma coisa no historico.
 */
public interface EmailTransport {

    /**
     * @return id da mensagem no transporte.
     * @throws EmailPermanenteException endereco invalido, caixa inexistente,
     *                                  recusa definitiva (5.x.x).
     * @throws EmailTransitorioException rede, timeout, 4.x.x, greylisting.
     */
    String enviar(ContaSmtp conta, String remetenteNome, String remetenteEndereco,
                  String destino, String assunto, String corpo);

    /**
     * Servidor de saida do tenant. Nulo quando a escola nao configurou SMTP —
     * nesse caso o envio cai no transporte de log.
     *
     * A senha chega ja' decifrada e NUNCA deve ser logada.
     */
    record ContaSmtp(String host, int porta, String usuario, String senha,
                     boolean starttls, boolean ssl) {

        public boolean utilizavel() {
            return host != null && !host.isBlank() && porta > 0;
        }
    }

    /** Falha que nao melhora com repeticao. */
    class EmailPermanenteException extends RuntimeException {
        private final String codigo;

        public EmailPermanenteException(String codigo, String mensagem) {
            super(mensagem);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }

    /** Falha que tende a melhorar em alguns minutos. */
    class EmailTransitorioException extends RuntimeException {
        private final String codigo;

        public EmailTransitorioException(String codigo, String mensagem, Throwable causa) {
            super(mensagem, causa);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
