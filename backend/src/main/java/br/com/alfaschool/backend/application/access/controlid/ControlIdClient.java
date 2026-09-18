package br.com.alfaschool.backend.application.access.controlid;

import br.com.alfaschool.backend.application.access.controlid.dto.ControlIdAccessLog;
import br.com.alfaschool.backend.application.access.controlid.dto.ControlIdUsuario;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP do firmware Control iD (iDFace / iDAccess).
 *
 * O firmware nao e' REST: tudo e' POST de JSON, a sessao vai na QUERY
 * STRING (nao em header), e varios erros chegam com HTTP 200.
 *
 * Diferencas deliberadas em relacao aos clientes anteriores do grupo:
 *  - UM RestClient com pool e timeouts (era HttpURLConnection copiado em
 *    seis metodos, alguns sem timeout);
 *  - JSON montado com ObjectMapper. Concatenar string quebrava no aluno
 *    chamado O'Brien e, com nome vindo de formulario, deixava injetar
 *    campo arbitrario no payload do equipamento;
 *  - sessao CACHEADA por dispositivo, invalidada em 401;
 *  - senha lida cifrada, SEM fallback silencioso para admin/admin.
 */
@Component
public class ControlIdClient {

    private static final Logger log = LoggerFactory.getLogger(ControlIdClient.class);

    /** Grupo 1 e' o "acesso livre" de fabrica do equipamento. */
    public static final int GRUPO_PADRAO = 1;

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final ControlIdSessaoCache sessoes;
    private final SegredoCifrador cifrador;

    public ControlIdClient(@Qualifier("controlIdRestClient") RestClient rest,
                           ObjectMapper mapper,
                           ControlIdSessaoCache sessoes,
                           SegredoCifrador cifrador) {
        this.rest = rest;
        this.mapper = mapper;
        this.sessoes = sessoes;
        this.cifrador = cifrador;
    }

    // =================================================================
    // Sessao
    // =================================================================

    /**
     * Devolve a sessao vigente, fazendo login apenas quando nao ha uma
     * valida em cache.
     */
    public String sessao(Dispositivo d) {
        String cacheada = sessoes.obter(d.getId());
        if (cacheada != null) {
            return cacheada;
        }
        return login(d);
    }

    /**
     * Login explicito. A senha vem cifrada do banco.
     *
     * Se decifrar devolver null (chave rotacionada sem reencriptar), a
     * operacao FALHA com mensagem clara. O fallback silencioso para
     * "admin" dos sistemas anteriores era pior que o erro: quando o
     * equipamento tinha a senha padrao ele funcionava, escondendo que o
     * cofre de segredos estava quebrado.
     */
    public String login(Dispositivo d) {
        String senha = senhaDo(d);
        String usuario = d.getLogin() == null || d.getLogin().isBlank() ? "admin" : d.getLogin();

        ObjectNode body = mapper.createObjectNode();
        body.put("login", usuario);
        body.put("password", senha);
        // Alguns firmwares exigem device_id; os que nao exigem ignoram.
        body.put("device_id", 1);

        ResponseEntity<String> resp = postJson(d.baseUrl() + "/login.fcgi", body);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException(
                    "Falha no login do equipamento " + d.getNome() + ": HTTP "
                            + resp.getStatusCode().value() + " " + resumo(resp.getBody()),
                    resp.getStatusCode().value(), null);
        }
        JsonNode json = ler(resp.getBody(), d);
        String token = json.path("session").asText(null);
        if (token == null || token.isBlank()) {
            throw new ControlIdException("Equipamento " + d.getNome()
                    + " não devolveu sessão no login. Verifique login e senha cadastrados.");
        }
        sessoes.guardar(d.getId(), token);
        return token;
    }

    private String senhaDo(Dispositivo d) {
        if (d.getSenhaCifrada() == null || d.getSenhaCifrada().length == 0) {
            throw new ControlIdException("Equipamento " + d.getNome()
                    + " está sem senha cadastrada. Cadastre a senha do leitor antes de sincronizar.");
        }
        String senha = cifrador.decifrar(d.getSenhaCifrada());
        if (senha == null) {
            throw new ControlIdException("Não foi possível decifrar a senha do equipamento "
                    + d.getNome() + ". A chave de cifra (APP_SECRET_KEY) provavelmente foi trocada; "
                    + "recadastre a senha do leitor.");
        }
        return senha;
    }

    // =================================================================
    // Objetos
    // =================================================================

    /**
     * Cria/atualiza a pessoa no equipamento e a vincula ao grupo de acesso.
     *
     * Vinculo duplicado responde HTTP 400 com "UNIQUE constraint failed:
     * user_groups" — isso e' sucesso disfarcado de erro e e' tratado como
     * tal. Qualquer outro 400 propaga.
     */
    public void sincronizarUsuario(Dispositivo d, ControlIdUsuario usuario) {
        ObjectNode valores = mapper.createObjectNode();
        valores.put("id", usuario.id());
        valores.put("registration", usuario.registration());
        valores.put("name", usuario.nome());
        criarObjetos(d, "users", List.of(valores), true);

        ObjectNode vinculo = mapper.createObjectNode();
        vinculo.put("user_id", usuario.id());
        vinculo.put("group_id", grupoDe(d));
        criarObjetos(d, "user_groups", List.of(vinculo), true);
    }

    /**
     * Revoga o acesso desvinculando a pessoa do grupo. O usuario e a face
     * permanecem no leitor para o caso de reativacao.
     *
     * NAO engole falha. Reportar "acesso revogado" quando o comando nao
     * chegou ao leitor significa uma crianca com restricao judicial ainda
     * passando pela catraca — a excecao sobe e quem chamou decide.
     */
    public void revogarAcesso(Dispositivo d, long deviceUserId) {
        ObjectNode where = mapper.createObjectNode();
        ObjectNode filtro = where.putObject("user_groups");
        filtro.put("user_id", deviceUserId);
        filtro.put("group_id", grupoDe(d));

        ObjectNode body = mapper.createObjectNode();
        body.put("object", "user_groups");
        body.set("where", where);

        ResponseEntity<String> resp = destruir(d, body);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Não foi possível revogar o acesso do usuário "
                    + deviceUserId + " no equipamento " + d.getNome() + ": HTTP "
                    + resp.getStatusCode().value() + " " + resumo(resp.getBody()),
                    resp.getStatusCode().value(), null);
        }
        validarRespostaGenerica(resp.getBody(), d,
                "Revogação de acesso recusada pelo equipamento " + d.getNome());
    }

    /** Remocao definitiva da pessoa do equipamento (users + vinculos + face). */
    public void removerUsuario(Dispositivo d, long deviceUserId) {
        ObjectNode whereVinculo = mapper.createObjectNode();
        whereVinculo.putObject("user_groups").put("user_id", deviceUserId);
        ObjectNode bodyVinculo = mapper.createObjectNode();
        bodyVinculo.put("object", "user_groups");
        bodyVinculo.set("where", whereVinculo);
        // Desvincula ANTES de apagar: se a remocao do usuario falhar no
        // meio, a pessoa ja esta sem acesso — fail-safe na ordem certa.
        ResponseEntity<String> r1 = destruir(d, bodyVinculo);
        if (!r1.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Falha ao desvincular usuário " + deviceUserId
                    + " no equipamento " + d.getNome() + ": " + resumo(r1.getBody()),
                    r1.getStatusCode().value(), null);
        }

        ObjectNode whereUser = mapper.createObjectNode();
        whereUser.putObject("users").put("id", deviceUserId);
        ObjectNode bodyUser = mapper.createObjectNode();
        bodyUser.put("object", "users");
        bodyUser.set("where", whereUser);
        ResponseEntity<String> r2 = destruir(d, bodyUser);
        if (!r2.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Falha ao remover usuário " + deviceUserId
                    + " no equipamento " + d.getNome() + ": " + resumo(r2.getBody()),
                    r2.getStatusCode().value(), null);
        }
    }

    private void criarObjetos(Dispositivo d, String objeto, List<ObjectNode> valores,
                              boolean toleraDuplicado) {
        ObjectNode body = mapper.createObjectNode();
        body.put("object", objeto);
        body.putArray("values").addAll(valores);

        ResponseEntity<String> resp = comSessao(d, sessao ->
                postJson(d.baseUrl() + "/create_objects.fcgi?session=" + sessao, body));

        if (resp.getStatusCode().is2xxSuccessful()) {
            return;
        }
        String corpo = resp.getBody() == null ? "" : resp.getBody().toLowerCase(Locale.ROOT);
        boolean duplicado = corpo.contains("unique") || corpo.contains("constraint")
                || corpo.contains("already") || corpo.contains("exists");
        if (toleraDuplicado && resp.getStatusCode().value() == 400 && duplicado) {
            // O firmware responde 400 para "ja existe". E' o estado desejado.
            log.debug("Objeto {} já existia no equipamento {}", objeto, d.getNome());
            return;
        }
        throw new ControlIdException("Falha ao criar " + objeto + " no equipamento "
                + d.getNome() + ": HTTP " + resp.getStatusCode().value() + " "
                + resumo(resp.getBody()), resp.getStatusCode().value(), null);
    }

    /**
     * destroy_objects.fcgi com fallback para delete_objects.fcgi.
     *
     * Firmwares antigos nao conhecem destroy_objects e respondem com o
     * texto "invalid command". Esse e' o unico papel de delete_objects.
     */
    private ResponseEntity<String> destruir(Dispositivo d, ObjectNode body) {
        ResponseEntity<String> resp = comSessao(d, sessao ->
                postJson(d.baseUrl() + "/destroy_objects.fcgi?session=" + sessao, body));
        String corpo = resp.getBody() == null ? "" : resp.getBody().toLowerCase(Locale.ROOT);
        if (corpo.contains("invalid command")) {
            log.debug("Equipamento {} não conhece destroy_objects; usando delete_objects", d.getNome());
            return comSessao(d, sessao ->
                    postJson(d.baseUrl() + "/delete_objects.fcgi?session=" + sessao, body));
        }
        return resp;
    }

    // =================================================================
    // Foto
    // =================================================================

    /**
     * Envia a foto de referencia.
     *
     * Formato exigido: query params session/user_id/timestamp,
     * Content-Type application/octet-stream e os BYTES JPEG crus no corpo.
     * Nao e' multipart nem base64 — o firmware devolve 200 com
     * success=false quando recebe qualquer outra coisa.
     *
     * A recusa vem estruturada em errors[0].code/message e vira
     * {@link FotoRecusadaException} com mensagem util em portugues.
     *
     * @throws FotoRecusadaException quando o leitor rejeita a imagem; a
     *         recusa e' definitiva para os mesmos bytes, nao retente.
     */
    public void enviarFoto(Dispositivo d, long deviceUserId, byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0) {
            throw new IllegalArgumentException("Foto vazia.");
        }
        long timestamp = System.currentTimeMillis() / 1000L;

        ResponseEntity<String> resp = comSessao(d, sessao -> rest.post()
                .uri(d.baseUrl() + "/user_set_image.fcgi?session=" + sessao
                        + "&user_id=" + deviceUserId + "&timestamp=" + timestamp)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(jpeg)
                .retrieve()
                .toEntity(String.class));

        String corpo = resp.getBody();

        if (!resp.getStatusCode().is2xxSuccessful()) {
            String texto = corpo == null ? "" : corpo;
            // "User does not exist": a linha de users ainda nao commitou no
            // equipamento. E' transitorio, nao recusa de imagem.
            if (texto.toLowerCase(Locale.ROOT).contains("user does not exist")) {
                throw new ControlIdException("Usuário " + deviceUserId + " ainda não existe no "
                        + "equipamento " + d.getNome() + "; sincronize a pessoa antes da foto.",
                        resp.getStatusCode().value(), null);
            }
            throw new ControlIdException("Falha ao enviar foto ao equipamento " + d.getNome()
                    + ": HTTP " + resp.getStatusCode().value() + " " + resumo(corpo),
                    resp.getStatusCode().value(), null);
        }

        // Sucesso pode vir com corpo vazio em alguns modelos.
        if (corpo == null || corpo.isBlank()) {
            return;
        }
        JsonNode json = ler(corpo, d);
        if (json.path("success").isBoolean() && json.path("success").asBoolean()) {
            return;
        }
        if (json.path("success").isBoolean() && !json.path("success").asBoolean()) {
            JsonNode erros = json.path("errors");
            Integer code = null;
            String message = null;
            if (erros.isArray() && !erros.isEmpty()) {
                JsonNode primeiro = erros.get(0);
                if (primeiro.hasNonNull("code")) {
                    code = primeiro.get("code").asInt();
                }
                message = primeiro.path("message").asText(null);
            }
            ControlIdFotoErros.Recusa recusa = ControlIdFotoErros.traduzir(code, message);
            log.info("Equipamento {} recusou a foto do usuário {}: {} ({})",
                    d.getNome(), deviceUserId, recusa.mensagem(), recusa.codigo());
            throw new FotoRecusadaException(recusa.codigo(), recusa.mensagem());
        }
    }

    /** Remove apenas a face; usuario e vinculos permanecem. */
    public void removerFoto(Dispositivo d, long deviceUserId) {
        ObjectNode body = mapper.createObjectNode();
        body.put("user_id", deviceUserId);
        ResponseEntity<String> resp = comSessao(d, sessao ->
                postJson(d.baseUrl() + "/user_destroy_image.fcgi?session=" + sessao
                        + "&user_id=" + deviceUserId, body));
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Falha ao remover a foto do usuário " + deviceUserId
                    + " no equipamento " + d.getNome() + ": " + resumo(resp.getBody()),
                    resp.getStatusCode().value(), null);
        }
    }

    // =================================================================
    // Acionamento
    // =================================================================

    /**
     * Abre porta / libera catraca.
     *
     * So' aceita {@link AcionamentoAcesso}, que ja validou o comando
     * contra a whitelist. Nunca receba a string de parametros direto da
     * API: o campo "parameters" e' livre no firmware.
     *
     * NAO e' idempotente. Timeout nao autoriza reenvio.
     */
    public void acionar(Dispositivo d, AcionamentoAcesso acionamento) {
        ObjectNode acao = mapper.createObjectNode();
        acao.put("action", acionamento.action());
        acao.put("parameters", acionamento.parametros());
        ObjectNode body = mapper.createObjectNode();
        body.putArray("actions").add(acao);

        ResponseEntity<String> resp = comSessao(d, sessao ->
                postJson(d.baseUrl() + "/execute_actions.fcgi?session=" + sessao, body));

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Falha ao acionar o equipamento " + d.getNome()
                    + ": HTTP " + resp.getStatusCode().value() + " " + resumo(resp.getBody()),
                    resp.getStatusCode().value(), null);
        }
        validarRespostaGenerica(resp.getBody(), d,
                "Acionamento recusado pelo equipamento " + d.getNome());
    }

    // =================================================================
    // Leitura de eventos
    // =================================================================

    /**
     * Le access_logs a partir de um id.
     *
     * O filtro e' por ID e nao por time: o firmware aplica o limit ANTES
     * de qualquer ordenacao e devolve na ordem de insercao. Filtrando por
     * tempo, o lote e' sempre o dos registros mais antigos e os eventos
     * novos nunca aparecem depois que o historico interno passa do limit.
     *
     * O operador ">" e' o unico comprovado em campo; "in" nao existe.
     */
    public List<ControlIdAccessLog> carregarAccessLogs(Dispositivo d, long aPartirDoId, int limite) {
        ObjectNode filtro = mapper.createObjectNode();
        filtro.putObject("access_logs").putObject("id").put(">", aPartirDoId);

        ObjectNode body = mapper.createObjectNode();
        body.put("object", "access_logs");
        body.putArray("fields")
                .add("id").add("user_id").add("time").add("event").add("device_id").add("portal_id");
        body.set("where", filtro);
        body.put("limit", limite);
        body.put("offset", 0);

        ResponseEntity<String> resp = comSessao(d, sessao ->
                postJson(d.baseUrl() + "/load_objects.fcgi?session=" + sessao, body));
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ControlIdException("Falha ao ler eventos do equipamento " + d.getNome()
                    + ": HTTP " + resp.getStatusCode().value() + " " + resumo(resp.getBody()),
                    resp.getStatusCode().value(), null);
        }
        JsonNode json = ler(resp.getBody(), d);
        List<ControlIdAccessLog> logs = new ArrayList<>();
        for (JsonNode linha : json.path("access_logs")) {
            logs.add(new ControlIdAccessLog(
                    linha.hasNonNull("id") ? linha.get("id").asLong() : null,
                    linha.hasNonNull("user_id") ? linha.get("user_id").asLong() : null,
                    linha.hasNonNull("time") ? linha.get("time").asLong() : null,
                    linha.hasNonNull("event") ? linha.get("event").asInt() : null,
                    linha.hasNonNull("device_id") ? linha.get("device_id").asInt() : null,
                    linha.hasNonNull("portal_id") ? linha.get("portal_id").asInt() : null,
                    linha.path("uhf_tag").asText(null)));
        }
        return logs;
    }

    // =================================================================
    // Infra interna
    // =================================================================

    private int grupoDe(Dispositivo d) {
        return d.getGrupoAcessoId() == null ? GRUPO_PADRAO : d.getGrupoAcessoId();
    }

    /**
     * Executa a chamada com a sessao em cache e, se o equipamento
     * responder 401/403, faz UM novo login e repete. Mais de uma
     * retentativa so' serve para bloquear a conta por tentativa invalida.
     */
    private ResponseEntity<String> comSessao(Dispositivo d,
                                             java.util.function.Function<String, ResponseEntity<String>> chamada) {
        String sessao = sessao(d);
        ResponseEntity<String> resp = chamada.apply(sessao);
        int status = resp.getStatusCode().value();
        if (status == 401 || status == 403) {
            sessoes.invalidar(d.getId());
            String nova = login(d);
            resp = chamada.apply(nova);
        }
        return resp;
    }

    private ResponseEntity<String> postJson(String url, ObjectNode body) {
        try {
            byte[] payload = mapper.writeValueAsBytes(body);
            return rest.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ControlIdException("Falha ao montar o payload para o equipamento.", e);
        } catch (Exception e) {
            throw new ControlIdException("Equipamento inacessível em " + url + ": " + e.getMessage(), e);
        }
    }

    private JsonNode ler(String corpo, Dispositivo d) {
        try {
            return mapper.readTree(corpo == null ? "{}" : corpo);
        } catch (Exception e) {
            throw new ControlIdException("Resposta ilegível do equipamento " + d.getNome()
                    + ": " + resumo(corpo), e);
        }
    }

    /**
     * Alguns firmwares respondem 200 com corpo vazio ou nao-JSON em caso
     * de sucesso; o que NAO se pode aceitar e' success=false ou um campo
     * error preenchido passando por sucesso.
     */
    private void validarRespostaGenerica(String corpo, Dispositivo d, String mensagemErro) {
        if (corpo == null || corpo.isBlank()) {
            return;
        }
        JsonNode json;
        try {
            json = mapper.readTree(corpo);
        } catch (Exception e) {
            return; // corpo simples, nao-JSON: o firmware sinalizou sucesso pelo 200
        }
        if (!json.isObject()) {
            return;
        }
        if (json.path("success").isBoolean() && !json.path("success").asBoolean()) {
            throw new ControlIdException(mensagemErro + ": " + resumo(corpo));
        }
        JsonNode erro = json.path("error");
        if (!erro.isMissingNode() && !erro.isNull() && !erro.asText("").isBlank()) {
            throw new ControlIdException(mensagemErro + ": " + erro.asText());
        }
        JsonNode erros = json.path("errors");
        if (erros.isArray() && !erros.isEmpty()) {
            throw new ControlIdException(mensagemErro + ": " + resumo(corpo));
        }
        String resultado = json.path("result").asText("");
        if (Map.of("error", 1, "fail", 1, "failed", 1, "denied", 1, "failure", 1)
                .containsKey(resultado.toLowerCase(Locale.ROOT))) {
            throw new ControlIdException(mensagemErro + ": " + resultado);
        }
    }

    /** Corta o corpo para o log: firmware as vezes despeja HTML inteiro. */
    private String resumo(String corpo) {
        if (corpo == null) {
            return "(sem corpo)";
        }
        String limpo = corpo.replaceAll("\\s+", " ").trim();
        return limpo.length() <= 300 ? limpo : limpo.substring(0, 300) + "...";
    }

    /** Exposto para o simulador e testes montarem bytes de foto validos. */
    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** Ajuda a diagnosticar cache de sessao em suporte. */
    public UUID idDe(Dispositivo d) {
        return d.getId();
    }
}
