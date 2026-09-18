package br.com.alfaschool.backend.access.controlid;

import br.com.alfaschool.backend.application.access.controlid.ControlIdWebhookController;
import br.com.alfaschool.backend.application.access.controlid.WebhookTokenService;
import br.com.alfaschool.backend.application.access.evento.EventoIngestaoService;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Autenticacao do webhook do equipamento.
 *
 * O ponto que nao pode regredir: o segredo e' POR DISPOSITIVO. No AlfaGym
 * havia um unico segredo global e quem o tivesse postava evento em nome
 * de qualquer equipamento de qualquer escola.
 */
class WebhookTokenServiceTest {

    private DispositivoRepository dispositivos;
    private EventoIngestaoService ingestao;
    private ControlIdWebhookController controller;
    private ObjectMapper mapper;

    private UUID dispositivoId;
    private Dispositivo dispositivo;
    private static final String TOKEN_VALIDO = "8dNkQ2lX-token-de-teste_9";

    @BeforeEach
    void preparar() {
        dispositivos = mock(DispositivoRepository.class);
        ingestao = mock(EventoIngestaoService.class);
        mapper = new ObjectMapper();
        controller = new ControlIdWebhookController(dispositivos, ingestao, mapper);

        dispositivoId = UUID.randomUUID();
        dispositivo = new Dispositivo();
        ReflectionTestUtils.setField(dispositivo, "id", dispositivoId);
        dispositivo.setTenantId(UUID.randomUUID());
        dispositivo.setDeleted(false);
        dispositivo.setAtivo(true);
        dispositivo.setNome("Leitor Portaria");
        dispositivo.setWebhookTokenHash(WebhookTokenService.hash(TOKEN_VALIDO));

        when(dispositivos.findById(dispositivoId)).thenReturn(Optional.of(dispositivo));
    }

    // =================================================================
    // Hash e comparacao
    // =================================================================

    @Test
    @DisplayName("hash e' SHA-256 hex de 64 caracteres e deterministico")
    void formatoDoHash() {
        String h = WebhookTokenService.hash(TOKEN_VALIDO);

        assertThat(h).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(h).isEqualTo(WebhookTokenService.hash(TOKEN_VALIDO));
        assertThat(h).isNotEqualTo(WebhookTokenService.hash(TOKEN_VALIDO + "x"));
    }

    @Test
    @DisplayName("comparacao aceita o token correto e recusa qualquer variacao")
    void comparacaoEmTempoConstante() {
        String hash = WebhookTokenService.hash(TOKEN_VALIDO);

        assertThat(WebhookTokenService.tokenConfere(hash, TOKEN_VALIDO)).isTrue();
        assertThat(WebhookTokenService.tokenConfere(hash, TOKEN_VALIDO + " ")).isFalse();
        assertThat(WebhookTokenService.tokenConfere(hash, TOKEN_VALIDO.toUpperCase())).isFalse();
        // Prefixo correto nao vale: a comparacao e' do buffer inteiro.
        assertThat(WebhookTokenService.tokenConfere(hash, TOKEN_VALIDO.substring(0, 5))).isFalse();
    }

    @Test
    @DisplayName("fail-closed: dispositivo sem token cadastrado nunca autentica")
    void semTokenCadastradoNegaTudo() {
        assertThat(WebhookTokenService.tokenConfere(null, TOKEN_VALIDO)).isFalse();
        assertThat(WebhookTokenService.tokenConfere("", TOKEN_VALIDO)).isFalse();
        assertThat(WebhookTokenService.tokenConfere(WebhookTokenService.hash(TOKEN_VALIDO), null)).isFalse();
        assertThat(WebhookTokenService.tokenConfere(null, null)).isFalse();
    }

    @Test
    @DisplayName("token de um dispositivo nao serve para outro")
    void tokenNaoAtravessaDispositivos() {
        String tokenDoOutroLeitor = "outro-token-completamente-diferente";

        assertThat(WebhookTokenService.tokenConfere(
                dispositivo.getWebhookTokenHash(), tokenDoOutroLeitor)).isFalse();
    }

    // =================================================================
    // Controller
    // =================================================================

    @Test
    @DisplayName("token errado devolve 401 e nao ingere nada")
    void tokenErradoDevolve401() throws Exception {
        JsonNode payload = mapper.readTree("""
                {"object_changes":[{"object":"access_logs","type":"inserted",
                 "values":{"id":1,"user_id":1001,"time":1780000000,"event":7}}]}""");

        assertThatThrownBy(() -> controller.receber(dispositivoId, "token-errado", payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(ingestao, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("sem header de token devolve 401")
    void semTokenDevolve401() throws Exception {
        JsonNode payload = mapper.readTree("{\"id\":1,\"user_id\":1001}");

        assertThatThrownBy(() -> controller.receber(dispositivoId, null, payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("dispositivo inexistente devolve 401, nao 404: nao entrega enumeracao")
    void dispositivoInexistenteDevolve401() throws Exception {
        UUID desconhecido = UUID.randomUUID();
        when(dispositivos.findById(desconhecido)).thenReturn(Optional.empty());
        JsonNode payload = mapper.readTree("{\"id\":1}");

        assertThatThrownBy(() -> controller.receber(desconhecido, TOKEN_VALIDO, payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // =================================================================
    // Formatos de payload
    // =================================================================

    @Test
    @DisplayName("envelope object_changes: so' access_logs inserted viram leitura")
    void envelopeNativoDoIdFace() throws Exception {
        JsonNode payload = mapper.readTree("""
                {"device_id":1,"object_changes":[
                  {"object":"access_logs","type":"inserted","values":{"id":10,"user_id":1001}},
                  {"object":"access_logs","type":"updated","values":{"id":11,"user_id":1002}},
                  {"object":"users","type":"inserted","values":{"id":12}}
                ]}""");

        List<JsonNode> leituras = controller.extrairLeituras(payload);

        assertThat(leituras).hasSize(1);
        assertThat(leituras.get(0).get("id").asLong()).isEqualTo(10);
    }

    @Test
    @DisplayName("array simples de eventos e' aceito")
    void arraySimples() throws Exception {
        JsonNode payload = mapper.readTree("[{\"id\":1},{\"id\":2}]");

        assertThat(controller.extrairLeituras(payload)).hasSize(2);
    }

    @Test
    @DisplayName("objeto unico e' aceito")
    void objetoUnico() throws Exception {
        JsonNode payload = mapper.readTree("{\"id\":1,\"user_id\":1001,\"event\":7}");

        assertThat(controller.extrairLeituras(payload)).hasSize(1);
    }
}
