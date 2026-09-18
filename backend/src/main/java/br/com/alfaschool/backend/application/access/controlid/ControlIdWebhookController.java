package br.com.alfaschool.backend.application.access.controlid;

import br.com.alfaschool.backend.application.access.evento.EventoIngestaoService;
import br.com.alfaschool.backend.application.access.evento.dto.LeituraBruta;
import br.com.alfaschool.backend.application.access.evento.dto.ResultadoIngestao;
import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recebe o push nativo do equipamento Control iD.
 *
 * ROTA PUBLICA POR NATUREZA: quem chama e' o firmware, que nao sabe
 * emitir JWT. A autenticacao e' feita AQUI DENTRO, por token proprio de
 * cada dispositivo, comparado em tempo constante. O SecurityConfig
 * precisa liberar POST /api/v1/access/webhook/** — ver relatorio.
 *
 * Nao existe TenantContext nesta requisicao (nao houve JWT), entao o
 * tenant vem do proprio dispositivo autenticado pelo token. E' por isso
 * que o segredo TEM de ser por equipamento: com segredo global, quem o
 * tivesse escolheria o tenant que quisesse na URL.
 */
@RestController
@RequestMapping("/api/v1/access/webhook")
public class ControlIdWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ControlIdWebhookController.class);

    private final DispositivoRepository dispositivos;
    private final EventoIngestaoService ingestao;
    private final ObjectMapper mapper;

    public ControlIdWebhookController(DispositivoRepository dispositivos,
                                      EventoIngestaoService ingestao,
                                      ObjectMapper mapper) {
        this.dispositivos = dispositivos;
        this.ingestao = ingestao;
        this.mapper = mapper;
    }

    /** Sonda de vida usada pelo instalador para conferir a URL no leitor. */
    @GetMapping("/{dispositivoId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(@PathVariable UUID dispositivoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Webhook ativo.",
                Map.of("dispositivoId", dispositivoId, "status", "ok")));
    }

    @PostMapping("/{dispositivoId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receber(
            @PathVariable UUID dispositivoId,
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
            @RequestBody JsonNode payload) {

        Dispositivo dispositivo = autenticar(dispositivoId, token);

        List<JsonNode> leituras = extrairLeituras(payload);
        List<UUID> gravados = new ArrayList<>();
        int replays = 0;
        List<String> erros = new ArrayList<>();

        for (JsonNode linha : leituras) {
            try {
                ResultadoIngestao r = ingestao.registrar(dispositivo.getTenantId(),
                        paraLeitura(dispositivo, linha));
                if (r.replay()) {
                    replays++;
                } else {
                    gravados.add(r.eventoId());
                }
            } catch (Exception e) {
                // Uma linha ruim no lote nao pode descartar as outras: o
                // firmware reenvia o lote inteiro, entao devolver 500
                // aqui multiplicaria o problema.
                log.warn("Falha ao ingerir leitura do equipamento {}: {}",
                        dispositivo.getNome(), e.getMessage());
                erros.add(e.getMessage());
            }
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("recebidos", leituras.size());
        corpo.put("gravados", gravados.size());
        corpo.put("replays", replays);
        corpo.put("eventoIds", gravados);
        corpo.put("erros", erros);
        return ResponseEntity.ok(ApiResponse.of(200, "Leituras processadas.", corpo));
    }

    /**
     * Autenticacao do equipamento.
     *
     * Devolve 401 tanto para dispositivo inexistente quanto para token
     * errado, e sem detalhar qual dos dois: distinguir daria a um
     * atacante um oraculo para enumerar dispositivos validos.
     */
    private Dispositivo autenticar(UUID dispositivoId, String token) {
        Dispositivo d = dispositivos.findById(dispositivoId)
                .filter(x -> Boolean.FALSE.equals(x.getDeleted()))
                .orElse(null);
        if (d == null || !WebhookTokenService.tokenConfere(d.getWebhookTokenHash(), token)) {
            log.warn("Webhook rejeitado para dispositivo {}", dispositivoId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de webhook inválido.");
        }
        if (!d.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Equipamento desativado.");
        }
        return d;
    }

    /**
     * Aceita os tres formatos que aparecem em campo:
     *  1. envelope nativo do iDFace:
     *     {"object_changes":[{"object":"access_logs","type":"inserted","values":{...}}]}
     *  2. array simples de eventos;
     *  3. objeto unico.
     *
     * No envelope, so' entram linhas de access_logs com type ausente ou
     * "inserted": "updated"/"deleted" de outras tabelas chegam no mesmo
     * canal e virariam passagens fantasma.
     */
    public List<JsonNode> extrairLeituras(JsonNode payload) {
        List<JsonNode> saida = new ArrayList<>();
        if (payload == null || payload.isNull()) {
            return saida;
        }
        if (payload.isArray()) {
            payload.forEach(saida::add);
            return saida;
        }
        JsonNode mudancas = payload.path("object_changes");
        if (mudancas.isArray()) {
            for (JsonNode m : mudancas) {
                if (!"access_logs".equals(m.path("object").asText(""))) {
                    continue;
                }
                String tipo = m.path("type").asText("inserted");
                if (!"inserted".equals(tipo)) {
                    continue;
                }
                JsonNode valores = m.path("values");
                if (valores.isObject()) {
                    saida.add(valores);
                } else if (valores.isArray()) {
                    valores.forEach(saida::add);
                }
            }
            return saida;
        }
        for (String chave : new String[]{"access_logs", "events", "logs", "notifications", "items"}) {
            JsonNode lista = payload.path(chave);
            if (lista.isArray()) {
                lista.forEach(saida::add);
                return saida;
            }
        }
        if (payload.isObject()) {
            saida.add(payload);
        }
        return saida;
    }

    private LeituraBruta paraLeitura(Dispositivo dispositivo, JsonNode linha) {
        Long logId = numero(linha, "id", "log_id", "event_id");
        Long userId = numero(linha, "user_id", "userId");
        Long epoch = numero(linha, "time", "timestamp", "datetime");
        Integer event = linha.hasNonNull("event") ? linha.get("event").asInt() : null;

        String raw;
        try {
            raw = mapper.writeValueAsString(linha);
        } catch (Exception e) {
            raw = null;
        }

        return new LeituraBruta(
                dispositivo.getId(),
                logId,
                userId,
                epoch,
                null,
                event,
                null,
                TipoIdentificacao.FACE,
                null,
                null,
                OrigemEvento.WEBHOOK,
                raw);
    }

    private Long numero(JsonNode linha, String... chaves) {
        for (String c : chaves) {
            JsonNode n = linha.get(c);
            if (n != null && n.isNumber()) {
                return n.asLong();
            }
            if (n != null && n.isTextual() && n.asText().matches("\\d+")) {
                return Long.parseLong(n.asText());
            }
        }
        return null;
    }
}
