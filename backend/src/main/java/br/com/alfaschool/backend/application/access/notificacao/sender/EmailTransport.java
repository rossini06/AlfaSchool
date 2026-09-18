package br.com.alfaschool.backend.application.access.notificacao.sender;

/**
 * Camada fina de transporte de e-mail.
 *
 * <p>MOTIVO DE EXISTIR: o projeto ainda NAO tem
 * {@code spring-boot-starter-mail} no pom, e alterar o pom nao faz parte desta
 * fatia. Em vez de deixar o canal de e-mail inexistente, o {@link EmailSender}
 * depende desta interface e hoje roda com {@link LogEmailTransport}, que
 * registra a mensagem e devolve sucesso. O fluxo inteiro (fila, claim, retry,
 * historico) fica testavel.
 *
 * <p>Para ligar SMTP de verdade: adicionar
 * {@code spring-boot-starter-mail} ao pom e criar um
 * {@code SmtpEmailTransport implements EmailTransport} usando
 * {@code JavaMailSender}, anotado com {@code @Primary} ou condicionado por
 * propriedade. Nada mais muda.
 */
public interface EmailTransport {

    /**
     * @return id da mensagem no transporte.
     * @throws EmailPermanenteException endereco invalido, caixa inexistente,
     *                                  recusa definitiva (5.x.x).
     * @throws EmailTransitorioException rede, timeout, 4.x.x, greylisting.
     */
    String enviar(String remetenteNome, String remetenteEndereco, String destino,
                  String assunto, String corpo);

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
