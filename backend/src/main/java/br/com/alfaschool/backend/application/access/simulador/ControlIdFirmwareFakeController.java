package br.com.alfaschool.backend.application.access.simulador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servidor FALSO do firmware Control iD.
 *
 * Responde os .fcgi como um iDFace responderia, para que o
 * ControlIdClient possa ser exercitado fim a fim contra localhost: basta
 * cadastrar um dispositivo com ip=127.0.0.1 e porta=<porta do backend>.
 *
 * O valor deste simulador esta em imitar os ERROS do firmware, nao os
 * acertos: 400 com "UNIQUE constraint failed" para duplicado, 200 com
 * success=false para foto recusada, "invalid command" para
 * destroy_objects em firmware antigo, epoch calculado no horario local.
 * Um mock que so' devolve 200 provaria apenas que o codigo compila.
 *
 * NUNCA em producao: fica atras de app.access.simulador-habilitado, que
 * e' false por padrao. As rotas estao na RAIZ (/login.fcgi), entao um
 * ambiente com isto ligado aceitaria comandos de equipamento sem
 * autenticacao nenhuma.
 */
@RestController
@ConditionalOnProperty(name = "app.access.simulador-habilitado", havingValue = "true")
public class ControlIdFirmwareFakeController {

    private static final Logger log = LoggerFactory.getLogger(ControlIdFirmwareFakeController.class);

    private final FirmwareFakeState estado;
    private final ObjectMapper mapper;

    public ControlIdFirmwareFakeController(FirmwareFakeState estado, ObjectMapper mapper) {
        this.estado = estado;
        this.mapper = mapper;
        log.warn("SIMULADOR DE LEITOR CONTROL ID HABILITADO. Não use este perfil em produção.");
    }

    @PostMapping(value = "/login.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody JsonNode body) {
        String usuario = body.path("login").asText("");
        String senha = body.path("password").asText("");
        if (usuario.isBlank() || senha.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"invalid login\"}");
        }
        String sessao = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        estado.sessoes.add(sessao);
        ObjectNode resp = mapper.createObjectNode();
        resp.put("session", sessao);
        return ResponseEntity.ok(json(resp));
    }

    @PostMapping(value = "/create_objects.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createObjects(@RequestParam(required = false) String session,
                                                @RequestBody JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        String objeto = body.path("object").asText("");
        ArrayNode ids = mapper.createArrayNode();

        for (JsonNode valores : body.path("values")) {
            switch (objeto) {
                case "users" -> {
                    long id = valores.path("id").asLong();
                    estado.usuarios.put(id, valores.path("name").asText(""));
                    ids.add(id);
                }
                case "user_groups" -> {
                    long userId = valores.path("user_id").asLong();
                    int grupo = valores.path("group_id").asInt(1);
                    if (estado.vinculos.containsKey(userId)) {
                        // O firmware devolve o erro do SQLite cru. Cliente
                        // que nao souber ler isso trata sucesso como falha.
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("UNIQUE constraint failed: user_groups.user_id");
                    }
                    estado.vinculos.put(userId, grupo);
                    ids.add(userId);
                }
                default -> ids.add(1);
            }
        }
        ObjectNode resp = mapper.createObjectNode();
        resp.set("ids", ids);
        return ResponseEntity.ok(json(resp));
    }

    /**
     * Cadastro facial.
     *
     * Aceita ou recusa conforme {@code proximoErroDeFoto}, ajustavel pelo
     * endpoint de teste. A recusa sai no formato exato do firmware: HTTP
     * 200, success=false e errors[0].code/message.
     */
    @PostMapping(value = "/user_set_image.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> userSetImage(@RequestParam(required = false) String session,
                                               @RequestParam(name = "user_id") long userId,
                                               @RequestParam(required = false) Long timestamp,
                                               @RequestBody(required = false) byte[] imagem) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        if (!estado.usuarios.containsKey(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User does not exist");
        }
        Integer erro = estado.proximoErroDeFoto;
        if (erro != null) {
            ObjectNode resp = mapper.createObjectNode();
            resp.put("success", false);
            ObjectNode e = mapper.createObjectNode();
            e.put("code", erro);
            e.put("message", mensagemDeErro(erro));
            resp.putArray("errors").add(e);
            ObjectNode scores = resp.putObject("scores");
            scores.put("width", 240);
            scores.put("height", 320);
            scores.put("bounds_width", 40);
            scores.put("bounds_height", 50);
            return ResponseEntity.ok(json(resp));
        }
        estado.fotos.put(userId, Integer.toHexString(java.util.Arrays.hashCode(imagem)));
        ObjectNode resp = mapper.createObjectNode();
        resp.put("success", true);
        return ResponseEntity.ok(json(resp));
    }

    @PostMapping(value = "/user_destroy_image.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> userDestroyImage(@RequestParam(required = false) String session,
                                                   @RequestParam(name = "user_id", required = false) Long userIdQuery,
                                                   @RequestBody(required = false) JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        Long userId = userIdQuery != null ? userIdQuery
                : (body != null && body.hasNonNull("user_id") ? body.get("user_id").asLong() : null);
        if (userId != null) {
            estado.fotos.remove(userId);
        }
        // Firmware devolve corpo vazio aqui. Cliente que exigir JSON quebra.
        return ResponseEntity.ok("");
    }

    @PostMapping(value = "/destroy_objects.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> destroyObjects(@RequestParam(required = false) String session,
                                                 @RequestBody JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        if (estado.firmwareAntigo) {
            return ResponseEntity.ok("invalid command");
        }
        return aplicarRemocao(body);
    }

    @PostMapping(value = "/delete_objects.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteObjects(@RequestParam(required = false) String session,
                                                @RequestBody JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        return aplicarRemocao(body);
    }

    private ResponseEntity<String> aplicarRemocao(JsonNode body) {
        String objeto = body.path("object").asText("");
        JsonNode where = body.path("where").path(objeto);
        if ("user_groups".equals(objeto) && where.hasNonNull("user_id")) {
            estado.vinculos.remove(where.get("user_id").asLong());
        } else if ("users".equals(objeto) && where.hasNonNull("id")) {
            long id = where.get("id").asLong();
            estado.usuarios.remove(id);
            estado.vinculos.remove(id);
            estado.fotos.remove(id);
        }
        ObjectNode resp = mapper.createObjectNode();
        resp.put("success", true);
        return ResponseEntity.ok(json(resp));
    }

    @PostMapping(value = "/execute_actions.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> executeActions(@RequestParam(required = false) String session,
                                                 @RequestBody JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        for (JsonNode acao : body.path("actions")) {
            String parametros = acao.path("parameters").asText("");
            // Armadilha real reproduzida: "id=1" responde 200 e NAO aciona
            // nada. Um cliente que mandasse id em vez de door acharia que
            // abriu a porta.
            if ("door".equals(acao.path("action").asText("")) && parametros.startsWith("id=")) {
                log.warn("Simulador: comando door com 'id=' ignorado (o firmware faz o mesmo).");
            }
        }
        // Firmware responde corpo vazio em caso de sucesso.
        return ResponseEntity.ok("");
    }

    @PostMapping(value = "/load_objects.fcgi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> loadObjects(@RequestParam(required = false) String session,
                                              @RequestBody JsonNode body) {
        ResponseEntity<String> semSessao = exigirSessao(session);
        if (semSessao != null) {
            return semSessao;
        }
        String objeto = body.path("object").asText("");
        ObjectNode resp = mapper.createObjectNode();
        ArrayNode linhas = resp.putArray(objeto);

        if ("access_logs".equals(objeto)) {
            long desde = body.path("where").path("access_logs").path("id").path(">").asLong(0);
            int limite = body.path("limit").asInt(200);
            synchronized (estado.accessLogs) {
                for (Map<String, Object> linha : estado.accessLogs) {
                    long id = ((Number) linha.get("id")).longValue();
                    if (id <= desde) {
                        continue;
                    }
                    // limit aplicado ANTES de ordenar, como no firmware.
                    if (linhas.size() >= limite) {
                        break;
                    }
                    linhas.add(mapper.valueToTree(linha));
                }
            }
        } else if ("users".equals(objeto)) {
            estado.usuarios.forEach((id, nome) -> {
                ObjectNode u = mapper.createObjectNode();
                u.put("id", id);
                u.put("name", nome);
                u.put("registration", String.valueOf(id));
                linhas.add(u);
            });
        }
        return ResponseEntity.ok(json(resp));
    }

    // =================================================================
    // Controle do simulador (fora do protocolo do firmware)
    // =================================================================

    /** Injeta uma passagem no historico do leitor falso. */
    public long registrarPassagem(long userId, int event, long epochLocal) {
        long id = estado.proximoLogId.getAndIncrement();
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", id);
        linha.put("user_id", userId);
        linha.put("time", epochLocal);
        linha.put("event", event);
        linha.put("device_id", 1);
        linha.put("portal_id", 1);
        estado.accessLogs.add(linha);
        return id;
    }

    private ResponseEntity<String> exigirSessao(String session) {
        if (session == null || !estado.sessoes.contains(session)) {
            // 401 e' o gatilho que faz o cliente invalidar o cache de
            // sessao e refazer login. Sem isso o cache nunca seria
            // exercitado no caminho de expiracao.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"invalid session\"}");
        }
        return null;
    }

    private String mensagemDeErro(int code) {
        return switch (code) {
            case 2 -> "Face not detected";
            case 3 -> "Face exists";
            case 4 -> "Face not centered";
            case 5 -> "Face too distant";
            case 6 -> "Face too close";
            case 7 -> "Face not centered, bad pose";
            case 8 -> "Low sharpness";
            case 9 -> "Face too close to image borders";
            default -> "Invalid image file";
        };
    }

    private String json(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}
