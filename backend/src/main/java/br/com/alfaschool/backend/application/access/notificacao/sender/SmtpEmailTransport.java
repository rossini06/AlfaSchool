package br.com.alfaschool.backend.application.access.notificacao.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.UUID;

/**
 * Envio real por SMTP, com a conta da propria escola.
 *
 * <h2>Por que um JavaMailSender por chamada</h2>
 * Cada escola tem o seu servidor de saida. Um bean unico obrigaria a
 * escolher uma conta para todo mundo, e o aviso sairia com o remetente
 * errado — ou nem sairia. O custo de montar o sender por envio e' baixo
 * perto de uma conexao SMTP.
 *
 * <h2>Classificacao do erro</h2>
 * E' o que decide se a mensagem volta para a fila ou morre ali. Autenticacao
 * recusada e endereco inexistente nao melhoram repetindo: insistir so' gasta
 * cota e atrasa o resto da fila. Timeout e queda de rede, sim.
 */
@Component
@Primary // ha dois EmailTransport; este decide, e cai no log quando nao ha conta
public class SmtpEmailTransport implements EmailTransport {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailTransport.class);

    private final LogEmailTransport fallback;

    public SmtpEmailTransport(LogEmailTransport fallback) {
        this.fallback = fallback;
    }

    @Override
    public String enviar(ContaSmtp conta, String remetenteNome, String remetenteEndereco,
                         String destino, String assunto, String corpo) {
        if (conta == null || !conta.utilizavel()) {
            // Sem SMTP configurado, registra em log — e o historico vai
            // mostrar o id "log:", para ninguem confundir com entrega real.
            return fallback.enviar(conta, remetenteNome, remetenteEndereco, destino, assunto, corpo);
        }

        JavaMailSenderImpl sender = montar(conta);
        try {
            MimeMessage mensagem = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, false, "UTF-8");
            helper.setFrom(remetenteEndereco, remetenteNome);
            helper.setTo(destino);
            helper.setSubject(assunto == null ? "" : assunto);
            helper.setText(corpo == null ? "" : corpo, false);
            sender.send(mensagem);

            String id = mensagem.getMessageID();
            return id != null ? id : "smtp:" + UUID.randomUUID();

        } catch (MailAuthenticationException e) {
            // Credencial recusada nao melhora repetindo.
            throw new EmailPermanenteException("SMTP_AUTH", "Autenticacao recusada pelo servidor de e-mail");
        } catch (MailSendException e) {
            throw classificarEnvio(e);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailPermanenteException("SMTP_MENSAGEM", "Mensagem invalida: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new EmailTransitorioException("SMTP_FALHA", e.getMessage(), e);
        }
    }

    /**
     * Endereco invalido e' definitivo; o resto tratamos como transitorio.
     * Na duvida repetir e' melhor do que descartar um aviso por erro de
     * classificacao.
     */
    private RuntimeException classificarEnvio(MailSendException e) {
        for (Exception causa : e.getFailedMessages().values()) {
            if (causa instanceof SendFailedException falha
                    && falha.getInvalidAddresses() != null
                    && falha.getInvalidAddresses().length > 0) {
                return new EmailPermanenteException("ENDERECO_INVALIDO",
                        "Servidor recusou o endereco de destino");
            }
        }
        return new EmailTransitorioException("SMTP_ENVIO", e.getMessage(), e);
    }

    private JavaMailSenderImpl montar(ContaSmtp conta) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(conta.host());
        sender.setPort(conta.porta());
        sender.setDefaultEncoding("UTF-8");
        if (conta.usuario() != null && !conta.usuario().isBlank()) {
            sender.setUsername(conta.usuario());
            sender.setPassword(conta.senha());
        }

        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", String.valueOf(conta.usuario() != null && !conta.usuario().isBlank()));
        p.put("mail.smtp.starttls.enable", String.valueOf(conta.starttls()));
        if (conta.ssl()) {
            p.put("mail.smtp.ssl.enable", "true");
        }
        // Sem timeout, um servidor mudo prende a thread do worker e a fila
        // inteira para — no pico de saida isso significa a escola sem avisos.
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.writetimeout", "15000");
        return sender;
    }
}
