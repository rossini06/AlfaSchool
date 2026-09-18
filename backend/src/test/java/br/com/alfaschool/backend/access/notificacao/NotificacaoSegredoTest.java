package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.NotificacaoConfigService;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigResponse;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoConfigRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * O segredo do provedor nunca pode sair do servidor.
 *
 * <p>Os testes serializam a resposta em JSON de verdade: verificar campo a
 * campo deixaria passar um getter novo adicionado sem pensar.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoSegredoTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final String TOKEN = "EAAG-token-secreto-da-meta-cloud-api-123456";

    @Mock
    private AccNotificacaoConfigRepository configRepository;

    private NotificacaoConfigService service;
    private SegredoCifrador cifrador;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cifrador = new SegredoCifrador("chave-de-teste-com-mais-de-32-caracteres-ok");
        service = new NotificacaoConfigService(configRepository, cifrador);
        TenantContext.setTenantId(TENANT);
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.existsByTenantIdAndCanalAndDeletedFalse(any(), any())).thenReturn(false);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    private NotificacaoConfigRequest comSegredo() {
        return new NotificacaoConfigRequest(CanalNotificacao.WHATSAPP, "META_CLOUD",
                "123456789", null, TOKEN, 500, true);
    }

    @Test
    @DisplayName("o segredo nao aparece em nenhuma resposta de API, nem cifrado")
    void segredoNuncaAparece() throws Exception {
        NotificacaoConfigResponse resposta = service.criar(comSegredo());
        String json = mapper.writeValueAsString(resposta);

        assertThat(json).doesNotContain(TOKEN);
        assertThat(resposta.segredoConfigurado()).isTrue();
        assertThat(resposta.segredoMascarado()).isEqualTo("********");

        // Nem o blob cifrado vaza: ele tambem e' material sensivel.
        ArgumentCaptor<AccNotificacaoConfig> captor = ArgumentCaptor.forClass(AccNotificacaoConfig.class);
        Mockito.verify(configRepository).save(captor.capture());
        byte[] cifrado = captor.getValue().getSegredoCifrado();
        assertThat(cifrado).isNotNull();
        assertThat(json).doesNotContain(Base64.getEncoder().encodeToString(cifrado));
        assertThat(json).doesNotContain(new String(cifrado, StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("o segredo e' gravado cifrado, nunca em texto plano")
    void gravaCifrado() {
        service.criar(comSegredo());

        ArgumentCaptor<AccNotificacaoConfig> captor = ArgumentCaptor.forClass(AccNotificacaoConfig.class);
        Mockito.verify(configRepository).save(captor.capture());
        byte[] cifrado = captor.getValue().getSegredoCifrado();

        assertThat(new String(cifrado, StandardCharsets.ISO_8859_1)).doesNotContain(TOKEN);
        // E continua recuperavel para o provedor usar.
        assertThat(cifrador.decifrar(cifrado)).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("listagem tambem nao devolve segredo")
    void listagemNaoDevolveSegredo() throws Exception {
        AccNotificacaoConfig config = new AccNotificacaoConfig();
        config.setTenantId(TENANT);
        config.setCanal(CanalNotificacao.WHATSAPP);
        config.setSegredoCifrado(cifrador.cifrar(TOKEN));
        when(configRepository.findByTenantIdAndDeletedFalseOrderByCanalAsc(TENANT)).thenReturn(List.of(config));

        String json = mapper.writeValueAsString(service.listar());

        assertThat(json).doesNotContain(TOKEN);
        assertThat(json).doesNotContain(Base64.getEncoder().encodeToString(config.getSegredoCifrado()));
    }

    @Test
    @DisplayName("segredo nulo na atualizacao mantem o que ja estava gravado")
    void segredoNuloMantem() {
        AccNotificacaoConfig existente = new AccNotificacaoConfig();
        existente.setTenantId(TENANT);
        existente.setCanal(CanalNotificacao.WHATSAPP);
        byte[] jaGravado = cifrador.cifrar(TOKEN);
        existente.setSegredoCifrado(jaGravado);
        when(configRepository.findByIdAndTenantIdAndDeletedFalse(any(), any())).thenReturn(Optional.of(existente));

        service.atualizar(UUID.randomUUID(), new NotificacaoConfigRequest(
                CanalNotificacao.WHATSAPP, "META_CLOUD", "123456789", null, null, 500, true));

        assertThat(existente.getSegredoCifrado()).isEqualTo(jaGravado);
    }

    @Test
    @DisplayName("segredo vazio apaga a credencial")
    void segredoVazioApaga() {
        AccNotificacaoConfig existente = new AccNotificacaoConfig();
        existente.setTenantId(TENANT);
        existente.setCanal(CanalNotificacao.WHATSAPP);
        existente.setSegredoCifrado(cifrador.cifrar(TOKEN));
        when(configRepository.findByIdAndTenantIdAndDeletedFalse(any(), any())).thenReturn(Optional.of(existente));

        service.atualizar(UUID.randomUUID(), new NotificacaoConfigRequest(
                CanalNotificacao.WHATSAPP, "META_CLOUD", "123456789", null, "", 500, true));

        assertThat(existente.temSegredo()).isFalse();
    }
}
