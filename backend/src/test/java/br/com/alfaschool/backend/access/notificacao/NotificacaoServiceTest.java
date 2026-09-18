package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.EnvioFilaWriter;
import br.com.alfaschool.backend.application.access.notificacao.NotificacaoRenderer;
import br.com.alfaschool.backend.application.access.notificacao.NotificacaoService;
import br.com.alfaschool.backend.application.access.notificacao.NotificacaoWorker;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final UUID RESPONSAVEL = UUID.randomUUID();

    @Mock
    private AccNotificacaoPreferenciaRepository preferenciaRepository;
    @Mock
    private AccNotificacaoTemplateRepository templateRepository;
    @Mock
    private AccNotificacaoEnvioRepository envioRepository;
    @Mock
    private NotificacaoWorker worker;

    private NotificacaoService service;

    @BeforeEach
    void setUp() {
        // EnvioFilaWriter real: a idempotencia e' o comportamento sob teste.
        EnvioFilaWriter filaWriter = new EnvioFilaWriter(envioRepository);
        service = new NotificacaoService(preferenciaRepository, templateRepository,
                filaWriter, new NotificacaoRenderer(), worker);

        when(preferenciaRepository.buscarResponsaveisDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenReturn(List.of(new TestFixtures.Destinatario(RESPONSAVEL.toString(), "Ana Silva")));
        when(preferenciaRepository.buscarPessoasAutorizadasDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenReturn(List.of());
        when(preferenciaRepository.buscarNomeDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenReturn(Optional.of("Maria Silva"));
        when(templateRepository.findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
        when(envioRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void enfileirar(String chave, Map<String, String> variaveis) {
        service.enfileirar(TENANT, EventoNotificacao.ENTRADA_CONFIRMADA, null, null, ALUNO, variaveis, chave);
    }

    private void comOptIn() {
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, RESPONSAVEL, CanalNotificacao.EMAIL,
                        true, Instant.now())));
    }

    @Test
    @DisplayName("mesma chave de idempotencia duas vezes cria um unico envio")
    void idempotencia() {
        comOptIn();
        // Primeira chamada: nao existe. Segunda: ja existe — o reprocessamento
        // da fila offline do agente nao pode avisar a familia de novo.
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString()))
                .thenReturn(false, true);

        enfileirar("ENTRADA:evt-1", Map.of("hora", "07:42"));
        enfileirar("ENTRADA:evt-1", Map.of("hora", "07:42"));

        verify(envioRepository, times(1)).saveAndFlush(any(AccNotificacaoEnvio.class));
        verify(worker, times(1)).processar(any());
    }

    @Test
    @DisplayName("corrida no indice unico e' absorvida sem excecao e sem duplicar")
    void idempotenciaSobCorrida() {
        comOptIn();
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);
        when(envioRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry chave_idempotencia"));

        assertThatCode(() -> enfileirar("ENTRADA:evt-2", Map.of("hora", "07:42")))
                .doesNotThrowAnyException();

        verify(worker, never()).processar(any());
    }

    @Test
    @DisplayName("preferencia habilitada mas SEM opt_in_em nao gera envio")
    void semOptInNaoEnvia() {
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, RESPONSAVEL, CanalNotificacao.EMAIL,
                        true, null)));

        enfileirar("ENTRADA:evt-3", Map.of("hora", "07:42"));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("preferencia desabilitada (opt-out) nao gera envio mesmo com opt_in_em antigo")
    void optOutNaoEnvia() {
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, RESPONSAVEL, CanalNotificacao.EMAIL,
                        false, Instant.now().minusSeconds(86400))));

        enfileirar("ENTRADA:evt-4", Map.of("hora", "07:42"));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("sem nenhuma preferencia cadastrada nao envia: nao existe default de recebimento")
    void semPreferenciaNaoEnvia() {
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of());

        enfileirar("ENTRADA:evt-5", Map.of("hora", "07:42"));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("variavel com data-URI e' rejeitada e nada e' enfileirado")
    void variavelDataUriRejeitada() {
        comOptIn();
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-6", Map.of(
                "hora", "07:42",
                "anexo", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("variavel com base64 longo (foto/biometria) e' rejeitada")
    void variavelBase64LongoRejeitada() {
        comOptIn();
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-7", Map.of("hora", "07:42", "conteudo", "A".repeat(400)));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("variavel de nome proibido (foto) e' rejeitada mesmo com valor curto")
    void variavelNomeProibidoRejeitada() {
        comOptIn();
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-8", Map.of("hora", "07:42", "foto", "x.png"));

        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("enfileirar nunca lanca: falha de repositorio nao derruba o chamador")
    void nuncaLanca() {
        when(preferenciaRepository.buscarResponsaveisDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenThrow(new IllegalStateException("banco fora do ar"));
        when(preferenciaRepository.buscarPessoasAutorizadasDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenThrow(new IllegalStateException("banco fora do ar"));

        assertThatCode(() -> enfileirar("ENTRADA:evt-9", Map.of("hora", "07:42")))
                .doesNotThrowAnyException();
        verify(envioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("chave derivada inclui canal e titular para nao colidir entre destinatarios")
    void chaveDerivadaPorDestinatario() {
        UUID outroResponsavel = UUID.randomUUID();
        when(preferenciaRepository.buscarResponsaveisDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenReturn(List.of(
                        new TestFixtures.Destinatario(RESPONSAVEL.toString(), "Ana"),
                        new TestFixtures.Destinatario(outroResponsavel.toString(), "Bruno")));
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, RESPONSAVEL, CanalNotificacao.EMAIL,
                        true, Instant.now())));
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, outroResponsavel))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, outroResponsavel, CanalNotificacao.EMAIL,
                        true, Instant.now())));
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-10", Map.of("hora", "07:42"));

        ArgumentCaptor<AccNotificacaoEnvio> captor = ArgumentCaptor.forClass(AccNotificacaoEnvio.class);
        verify(envioRepository, times(2)).saveAndFlush(captor.capture());
        List<String> chaves = captor.getAllValues().stream()
                .map(AccNotificacaoEnvio::getChaveIdempotencia)
                .toList();
        assertThat(chaves).doesNotHaveDuplicates();
        assertThat(chaves).allMatch(c -> c.startsWith("ENTRADA:evt-10|EMAIL|"));
    }

    @Test
    @DisplayName("sem template cadastrado usa o texto de fabrica e renderiza as variaveis")
    void usaTemplatePadrao() {
        comOptIn();
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-11", Map.of("hora", "07:42", "data", "18/09/2026"));

        ArgumentCaptor<AccNotificacaoEnvio> captor = ArgumentCaptor.forClass(AccNotificacaoEnvio.class);
        verify(envioRepository).saveAndFlush(captor.capture());
        AccNotificacaoEnvio envio = captor.getValue();
        assertThat(envio.getCorpo()).contains("Maria Silva").contains("07:42");
        assertThat(envio.getCorpo()).doesNotContain("{");
        assertThat(envio.getAlunoId()).isEqualTo(ALUNO);
        assertThat(envio.getDestino()).isEqualTo("mae@exemplo.com");
    }

    @Test
    @DisplayName("pessoa autorizada com recebe_notificacao entra como destinatario AUTORIZADA")
    void pessoaAutorizadaTambemRecebe() {
        UUID autorizada = UUID.randomUUID();
        when(preferenciaRepository.buscarPessoasAutorizadasDoAluno(TENANT.toString(), ALUNO.toString()))
                .thenReturn(List.of(new TestFixtures.Destinatario(autorizada.toString(), "Tia Joana")));
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, RESPONSAVEL))
                .thenReturn(List.of());
        when(preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(TENANT, autorizada))
                .thenReturn(List.of(TestFixtures.preferencia(TENANT, autorizada, CanalNotificacao.EMAIL,
                        true, Instant.now())));
        when(envioRepository.existsByTenantIdAndChaveIdempotencia(eq(TENANT), anyString())).thenReturn(false);

        enfileirar("ENTRADA:evt-12", Map.of("hora", "07:42"));

        ArgumentCaptor<AccNotificacaoEnvio> captor = ArgumentCaptor.forClass(AccNotificacaoEnvio.class);
        verify(envioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTitularId()).isEqualTo(autorizada);
        assertThat(captor.getValue().getTitularTipo())
                .isEqualTo(br.com.alfaschool.backend.domain.access.shared.TitularTipo.AUTORIZADA);
    }
}
