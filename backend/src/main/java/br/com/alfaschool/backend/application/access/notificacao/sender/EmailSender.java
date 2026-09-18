package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de e-mail.
 *
 * <p>A conexao real fica atras de {@link EmailTransport} porque o pom ainda nao
 * tem {@code spring-boot-starter-mail} (ver javadoc da interface). Este sender
 * cuida do que e' dele: validar destino, escolher remetente a partir da
 * configuracao do tenant e traduzir excecao de transporte em
 * {@link ResultadoEnvio} classificado.
 */
@Component
public class EmailSender implements NotificacaoSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailTransport transport;

    public EmailSender(EmailTransport transport) {
        this.transport = transport;
    }

    @Override
    public CanalNotificacao canal() {
        return CanalNotificacao.EMAIL;
    }

    @Override
    public ResultadoEnvio enviar(EnvioRequest request) {
        String destino = request.destino();
        if (destino == null || destino.isBlank() || !destino.contains("@")) {
            return ResultadoEnvio.permanente("ENDERECO_INVALIDO", "Destino nao e' um e-mail valido");
        }

        AccNotificacaoConfig config = request.config();
        String remetente = config != null && config.getRemetente() != null && !config.getRemetente().isBlank()
                ? config.getRemetente() : "nao-responda@alfaschool.local";

        try {
            String id = transport.enviar("AlfaSchool", remetente, destino,
                    request.assunto(), request.corpo());
            return ResultadoEnvio.ok(id);
        } catch (EmailTransport.EmailPermanenteException e) {
            return ResultadoEnvio.permanente(e.getCodigo(), e.getMessage());
        } catch (EmailTransport.EmailTransitorioException e) {
            return ResultadoEnvio.transitorio(e.getCodigo(), e.getMessage());
        } catch (RuntimeException e) {
            // Desconhecido tratamos como transitorio: e' melhor tentar de novo
            // do que descartar um aviso por causa de um bug de classificacao.
            log.warn("Falha nao classificada no envio de e-mail para {}: {}", destino, e.toString());
            return ResultadoEnvio.transitorio("ERRO_TRANSPORTE", e.toString());
        }
    }
}
