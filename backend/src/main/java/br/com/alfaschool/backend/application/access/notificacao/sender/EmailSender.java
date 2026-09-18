package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de e-mail.
 *
 * <p>Cuida do que e' dele: validar destino, montar a conta SMTP da escola a
 * partir da configuracao do tenant — decifrando a senha no ultimo momento
 * possivel — e traduzir excecao de transporte em {@link ResultadoEnvio}
 * classificado. Conectar e' com o {@link EmailTransport}.
 */
@Component
public class EmailSender implements NotificacaoSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailTransport transport;
    private final br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador cifrador;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public EmailSender(EmailTransport transport,
                       br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador cifrador,
                       com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.transport = transport;
        this.cifrador = cifrador;
        this.objectMapper = objectMapper;
    }

    /**
     * Le a conta de saida da escola. O {@code config_json} guarda host, porta,
     * usuario e as opcoes de TLS; a SENHA vive apenas em {@code segredo_cifrado}
     * e e' decifrada aqui, no ultimo momento possivel.
     *
     * Devolve null quando nao ha SMTP configurado — o transporte entao registra
     * em log em vez de fingir que entregou.
     */
    private EmailTransport.ContaSmtp contaDe(AccNotificacaoConfig config) {
        if (config == null || config.getConfigJson() == null || config.getConfigJson().isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode n = objectMapper.readTree(config.getConfigJson());
            String host = n.path("host").asText(null);
            if (host == null || host.isBlank()) {
                return null;
            }
            String senha = cifrador.decifrar(config.getSegredoCifrado());
            if (config.getSegredoCifrado() != null && senha == null) {
                // Chave de cifra trocada sem reencriptar: melhor falhar alto
                // do que tentar autenticar com senha vazia e ser bloqueado.
                log.error("Nao foi possivel decifrar a senha SMTP do tenant {}. "
                        + "A chave APP_SECRET_KEY mudou sem reencriptar?", config.getTenantId());
                return null;
            }
            return new EmailTransport.ContaSmtp(
                    host,
                    n.path("porta").asInt(587),
                    n.path("usuario").asText(null),
                    senha,
                    n.path("starttls").asBoolean(true),
                    n.path("ssl").asBoolean(false));
        } catch (Exception e) {
            log.error("config_json invalido no canal de e-mail do tenant {}: {}",
                    config.getTenantId(), e.toString());
            return null;
        }
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
            String id = transport.enviar(contaDe(config), "AlfaSchool", remetente, destino,
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
