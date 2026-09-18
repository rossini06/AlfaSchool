package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * WhatsApp pela Meta Cloud API (API oficial).
 *
 * <h2>O que ainda falta para ligar</h2>
 * <ol>
 *   <li>Conta <b>Meta Business verificada</b> e um numero registrado na Cloud
 *       API (o {@code phone-number-id}).</li>
 *   <li><b>Template aprovado</b> pela Meta. Fora da janela de 24h de
 *       atendimento a API so' aceita mensagem do tipo {@code template}; texto
 *       livre e' recusado. O campo
 *       {@code acc_notificacao_templates.template_externo} guarda exatamente o
 *       NOME desse template aprovado, e e' o que viaja em
 *       {@code template.name}.</li>
 *   <li><b>Token permanente</b> do app, gravado cifrado em
 *       {@code acc_notificacao_configs.segredo_cifrado}.</li>
 * </ol>
 *
 * <p><b>Custo:</b> a Meta cobra por conversa iniciada, por categoria
 * (utility/authentication/marketing). Avisos de entrada e saida caem em
 * utility. O teto diario de {@code acc_notificacao_configs.limite_diario}
 * existe justamente para a escola nao descobrir a cobranca no fim do mes.
 *
 * <p><b>Nao ha credencial neste codigo.</b> Tudo vem da configuracao do tenant,
 * decifrada por {@link SegredoCifrador} no momento do envio e nunca logada.
 *
 * <p>Payload enviado (POST {@code /{versao}/{phone-number-id}/messages}):
 * <pre>
 * {
 *   "messaging_product": "whatsapp",
 *   "to": "5527999999999",
 *   "type": "template",
 *   "template": {
 *     "name": "&lt;template_externo&gt;",
 *     "language": { "code": "pt_BR" },
 *     "components": [
 *       { "type": "body", "parameters": [ {"type":"text","text":"..."} ] }
 *     ]
 *   }
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "app.access.notificacao.whatsapp.provider", havingValue = "META_CLOUD")
public class MetaCloudWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(MetaCloudWhatsAppProvider.class);

    private static final String ENDPOINT_PADRAO = "https://graph.facebook.com";
    private static final String VERSAO_PADRAO = "v20.0";

    private final SegredoCifrador segredoCifrador;
    private final RestClient restClient;

    public MetaCloudWhatsAppProvider(SegredoCifrador segredoCifrador) {
        this.segredoCifrador = segredoCifrador;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String nome() {
        return "META_CLOUD";
    }

    @Override
    public ResultadoEnvio enviar(EnvioRequest request) {
        AccNotificacaoConfig config = request.config();
        if (config == null) {
            return ResultadoEnvio.permanente("SEM_CONFIG", "Canal WHATSAPP sem configuracao para o tenant");
        }

        // phone-number-id do numero registrado na Cloud API.
        String phoneNumberId = config.getRemetente();
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return ResultadoEnvio.permanente("SEM_PHONE_NUMBER_ID",
                    "Configure o phone-number-id na coluna remetente da configuracao do canal");
        }

        String token;
        try {
            token = segredoCifrador.decifrar(config.getSegredoCifrado());
        } catch (RuntimeException e) {
            // Chave de cifra trocada ou segredo corrompido: repetir nao resolve.
            return ResultadoEnvio.permanente("SEGREDO_ILEGIVEL", "Nao foi possivel decifrar o token do provedor");
        }
        if (token == null || token.isBlank()) {
            return ResultadoEnvio.permanente("SEM_CREDENCIAL",
                    "Token da Meta Cloud API nao cadastrado para este tenant");
        }

        String templateNome = request.templateExterno();
        if (templateNome == null || templateNome.isBlank()) {
            // Sem template aprovado a Meta recusa a mensagem fora da janela de 24h.
            return ResultadoEnvio.permanente("SEM_TEMPLATE_EXTERNO",
                    "Cadastre o nome do template aprovado em acc_notificacao_templates.template_externo");
        }

        String destino = normalizarE164(request.destino());
        if (destino == null) {
            return ResultadoEnvio.permanente("NUMERO_INVALIDO", "Numero invalido: " + request.destino());
        }

        // Endpoint alternativo (sandbox, proxy) vem de config_json; sem isso,
        // usamos o endpoint oficial.
        String url = ENDPOINT_PADRAO + "/" + VERSAO_PADRAO + "/" + phoneNumberId + "/messages";

        Map<String, Object> corpoJson = montarPayload(destino, templateNome, request.variaveis());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoJson)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        // Nao lancamos aqui: a classificacao acontece abaixo.
                    })
                    .body(Map.class);

            String messageId = extrairMessageId(resposta);
            if (messageId == null) {
                // Resposta 2xx sem id costuma ser erro de negocio embutido.
                return classificarErroMeta(resposta);
            }
            return ResultadoEnvio.ok(messageId);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Timeout / DNS / conexao: tipicamente transitorio.
            return ResultadoEnvio.transitorio("REDE", e.toString());
        } catch (RuntimeException e) {
            log.warn("Falha ao chamar a Meta Cloud API: {}", e.toString());
            return ResultadoEnvio.transitorio("ERRO_PROVIDER", e.toString());
        }
    }

    /**
     * Monta {@code template.components}. A Meta posiciona os parametros por
     * ordem, nao por nome: ordenamos as variaveis alfabeticamente para que o
     * mapeamento seja estavel entre execucoes.
     */
    private Map<String, Object> montarPayload(String destino, String templateNome, Map<String, String> variaveis) {
        List<Map<String, String>> parametros = new ArrayList<>();
        if (variaveis != null) {
            for (Map.Entry<String, String> e : new TreeMap<>(variaveis).entrySet()) {
                parametros.add(Map.of("type", "text", "text", e.getValue() == null ? "" : e.getValue()));
            }
        }

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateNome);
        template.put("language", Map.of("code", "pt_BR"));
        if (!parametros.isEmpty()) {
            template.put("components", List.of(Map.of("type", "body", "parameters", parametros)));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", destino);
        payload.put("type", "template");
        payload.put("template", template);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private String extrairMessageId(Map<String, Object> resposta) {
        if (resposta == null) {
            return null;
        }
        Object messages = resposta.get("messages");
        if (messages instanceof List<?> lista && !lista.isEmpty()
                && lista.get(0) instanceof Map<?, ?> primeira) {
            Object id = ((Map<String, Object>) primeira).get("id");
            return id == null ? null : id.toString();
        }
        return null;
    }

    /**
     * Traduz o bloco {@code error} da Meta para permanente/transitorio.
     *
     * <p>Codigos relevantes: 131026 (numero nao existe no WhatsApp) e 132xxx
     * (problemas de template) sao permanentes; 80007 (rate limit), 130429
     * (throughput) e 1 (erro interno) sao transitorios.
     */
    @SuppressWarnings("unchecked")
    private ResultadoEnvio classificarErroMeta(Map<String, Object> resposta) {
        if (resposta == null) {
            return ResultadoEnvio.transitorio("RESPOSTA_VAZIA", "Meta nao devolveu corpo");
        }
        Object erro = resposta.get("error");
        if (!(erro instanceof Map)) {
            return ResultadoEnvio.transitorio("RESPOSTA_INESPERADA", String.valueOf(resposta));
        }
        Map<String, Object> mapaErro = (Map<String, Object>) erro;
        String codigo = String.valueOf(mapaErro.get("code"));
        String mensagem = String.valueOf(mapaErro.getOrDefault("message", "erro Meta"));

        boolean permanente = switch (codigo) {
            case "131026", "131047", "131051", "132000", "132001", "132005",
                 "132007", "132012", "132015", "133010", "190" -> true;
            default -> false;
        };
        return permanente
                ? ResultadoEnvio.permanente("META_" + codigo, mensagem)
                : ResultadoEnvio.transitorio("META_" + codigo, mensagem);
    }

    /** A Meta espera o numero so' com digitos, com DDI. */
    private String normalizarE164(String bruto) {
        if (bruto == null) {
            return null;
        }
        String digitos = bruto.replaceAll("\\D", "");
        if (digitos.length() < 10) {
            return null;
        }
        if (digitos.length() <= 11) {
            // Numero nacional sem DDI: assumimos Brasil.
            digitos = "55" + digitos;
        }
        return digitos;
    }
}
