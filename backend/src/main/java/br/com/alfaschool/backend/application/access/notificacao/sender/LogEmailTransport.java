package br.com.alfaschool.backend.application.access.notificacao.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Transporte de e-mail funcional que escreve no log em vez de abrir conexao
 * SMTP. E' o default enquanto {@code spring-boot-starter-mail} nao entra no
 * pom.
 *
 * <p>Para ligar SMTP de verdade: acrescentar {@code spring-boot-starter-mail}
 * ao pom e registrar um {@code SmtpEmailTransport implements EmailTransport}
 * anotado com {@code @Primary}. Nada mais no motor muda.
 */
@Component
public class LogEmailTransport implements EmailTransport {

    private static final Logger log = LoggerFactory.getLogger(LogEmailTransport.class);

    @Override
    public String enviar(ContaSmtp conta, String remetenteNome, String remetenteEndereco, String destino,
                         String assunto, String corpo) {
        if (destino == null || !destino.contains("@")) {
            // Endereco quebrado nunca vai melhorar: erro permanente.
            throw new EmailPermanenteException("ENDERECO_INVALIDO", "Endereco de e-mail invalido: " + destino);
        }
        String id = "log-email-" + UUID.randomUUID();
        log.info("[NOTIFICACAO][EMAIL][{}] de={} <{}> para={} assunto={} corpo={}",
                id, remetenteNome, remetenteEndereco, destino, assunto, corpo);
        return id;
    }
}
