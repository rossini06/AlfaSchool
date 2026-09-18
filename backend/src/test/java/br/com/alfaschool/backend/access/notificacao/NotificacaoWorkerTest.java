package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.EnvioFilaWriter;
import br.com.alfaschool.backend.application.access.notificacao.NotificacaoWorker;
import br.com.alfaschool.backend.application.access.notificacao.sender.EnvioRequest;
import br.com.alfaschool.backend.application.access.notificacao.sender.NotificacaoSender;
import br.com.alfaschool.backend.application.access.notificacao.sender.ResultadoEnvio;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoConfigRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoWorkerTest {

    private static final UUID TENANT = UUID.randomUUID();

    @Mock
    private AccNotificacaoEnvioRepository envioRepository;
    @Mock
    private AccNotificacaoConfigRepository configRepository;
    @Mock
    private AccNotificacaoTemplateRepository templateRepository;
    @Mock
    private EnvioFilaWriter filaWriter;

    private SenderEspiao sender;
    private NotificacaoWorker worker;

    /** Sender controlavel: registra chamadas e devolve o resultado programado. */
    static class SenderEspiao implements NotificacaoSender {
        final List<EnvioRequest> chamadas = new ArrayList<>();
        ResultadoEnvio resposta = ResultadoEnvio.ok("ok-1");

        @Override
        public CanalNotificacao canal() {
            return CanalNotificacao.EMAIL;
        }

        @Override
        public ResultadoEnvio enviar(EnvioRequest request) {
            chamadas.add(request);
            return resposta;
        }
    }

    @BeforeEach
    void setUp() {
        sender = new SenderEspiao();
        worker = new NotificacaoWorker(envioRepository, configRepository, templateRepository,
                filaWriter, List.of(sender));
        when(configRepository.findFirstByTenantIdAndCanalAndAtivoTrueAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(templateRepository.findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    private AccNotificacaoEnvio pendente(int tentativas) {
        return TestFixtures.envio(TENANT, CanalNotificacao.EMAIL, StatusEnvio.PENDENTE,
                tentativas, Instant.now());
    }

    @Test
    @DisplayName("claim atomico: com dois workers concorrentes apenas um envia")
    void claimAtomicoUmSoEnvia() {
        AccNotificacaoEnvio envio = pendente(0);
        // O primeiro worker consegue o UPDATE condicional; o segundo recebe 0
        // linhas afetadas e desiste.
        when(filaWriter.reivindicar(envio.getId()))
                .thenReturn(Optional.of(envio))
                .thenReturn(Optional.empty());

        worker.processarSincrono(envio.getId());
        worker.processarSincrono(envio.getId());

        assertThat(sender.chamadas).hasSize(1);
        verify(filaWriter, times(1)).registrarSucesso(eq(envio.getId()), any());
    }

    @Test
    @DisplayName("erro permanente marca FALHOU sem reagendar (agendadoPara nulo)")
    void erroPermanenteNaoReagenda() {
        AccNotificacaoEnvio envio = pendente(0);
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));
        sender.resposta = ResultadoEnvio.permanente("ENDERECO_INVALIDO", "e-mail nao existe");

        worker.processarSincrono(envio.getId());

        // proximaTentativa nulo = fora da fila para sempre.
        verify(filaWriter).registrarFalha(eq(envio.getId()), eq("ENDERECO_INVALIDO"), any(), isNull());
        verify(filaWriter, never()).reagendarSemPenalidade(any(), any(), any());
    }

    @Test
    @DisplayName("erro transitorio reagenda com backoff exponencial crescente")
    void erroTransitorioReagendaComBackoffCrescente() {
        sender.resposta = ResultadoEnvio.transitorio("REDE", "timeout");

        List<Duration> esperados = List.of(
                Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15));
        List<Duration> observados = new ArrayList<>();

        for (int tentativasJaFeitas = 0; tentativasJaFeitas < 3; tentativasJaFeitas++) {
            AccNotificacaoEnvio envio = pendente(tentativasJaFeitas);
            when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

            Instant antes = Instant.now();
            worker.processarSincrono(envio.getId());

            ArgumentCaptor<Instant> quando = ArgumentCaptor.forClass(Instant.class);
            verify(filaWriter).registrarFalha(eq(envio.getId()), eq("REDE"), any(), quando.capture());
            assertThat(quando.getValue()).as("transitorio tem de reagendar").isNotNull();
            observados.add(Duration.between(antes, quando.getValue()));
        }

        for (int i = 0; i < esperados.size(); i++) {
            // Tolerancia de 5s para o tempo gasto no proprio teste.
            assertThat(observados.get(i))
                    .isBetween(esperados.get(i).minusSeconds(5), esperados.get(i).plusSeconds(5));
        }
        assertThat(observados.get(0)).isLessThan(observados.get(1));
        assertThat(observados.get(1)).isLessThan(observados.get(2));
    }

    @Test
    @DisplayName("teto de tentativas transforma erro transitorio em falha definitiva")
    void tetoDeTentativas() {
        AccNotificacaoEnvio envio = pendente(NotificacaoWorker.MAX_TENTATIVAS - 1);
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));
        sender.resposta = ResultadoEnvio.transitorio("REDE", "timeout");

        worker.processarSincrono(envio.getId());

        verify(filaWriter).registrarFalha(eq(envio.getId()), eq("REDE"), any(), isNull());
    }

    @Test
    @DisplayName("teto diario do canal reagenda para o dia seguinte em vez de falhar")
    void tetoDiarioReagendaParaODiaSeguinte() {
        AccNotificacaoEnvio envio = pendente(0);
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

        AccNotificacaoConfig config = new AccNotificacaoConfig();
        config.setTenantId(TENANT);
        config.setCanal(CanalNotificacao.EMAIL);
        config.setLimiteDiario(100);
        when(configRepository.findFirstByTenantIdAndCanalAndAtivoTrueAndDeletedFalse(TENANT, CanalNotificacao.EMAIL))
                .thenReturn(Optional.of(config));
        when(envioRepository.contarEnviadosDesde(eq(TENANT), eq(CanalNotificacao.EMAIL), any()))
                .thenReturn(100L);

        worker.processarSincrono(envio.getId());

        ArgumentCaptor<Instant> quando = ArgumentCaptor.forClass(Instant.class);
        verify(filaWriter).reagendarSemPenalidade(eq(envio.getId()), quando.capture(), eq("TETO_DIARIO"));
        // Nao gasta tentativa e nao chama o provedor.
        assertThat(sender.chamadas).isEmpty();
        verify(filaWriter, never()).registrarFalha(any(), any(), any(), any());

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        assertThat(quando.getValue().atZone(fuso).toLocalDate())
                .isEqualTo(LocalDate.now(fuso).plusDays(1));
    }

    @Test
    @DisplayName("abaixo do teto diario o envio segue normalmente")
    void abaixoDoTetoEnvia() {
        AccNotificacaoEnvio envio = pendente(0);
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

        AccNotificacaoConfig config = new AccNotificacaoConfig();
        config.setTenantId(TENANT);
        config.setCanal(CanalNotificacao.EMAIL);
        config.setLimiteDiario(100);
        when(configRepository.findFirstByTenantIdAndCanalAndAtivoTrueAndDeletedFalse(TENANT, CanalNotificacao.EMAIL))
                .thenReturn(Optional.of(config));
        when(envioRepository.contarEnviadosDesde(eq(TENANT), eq(CanalNotificacao.EMAIL), any()))
                .thenReturn(99L);

        worker.processarSincrono(envio.getId());

        assertThat(sender.chamadas).hasSize(1);
    }

    @Test
    @DisplayName("envio criado ha mais de 24h expira sem chamar o provedor")
    void expiraApos24h() {
        AccNotificacaoEnvio envio = TestFixtures.envio(TENANT, CanalNotificacao.EMAIL, StatusEnvio.PENDENTE,
                0, Instant.now().minus(Duration.ofHours(25)));
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

        worker.processarSincrono(envio.getId());

        assertThat(sender.chamadas).isEmpty();
        verify(filaWriter).marcarExpirado(envio.getId());
    }

    @Test
    @DisplayName("envio com 23h ainda e' enviado: dentro da janela util")
    void dentroDaJanelaAindaEnvia() {
        AccNotificacaoEnvio envio = TestFixtures.envio(TENANT, CanalNotificacao.EMAIL, StatusEnvio.PENDENTE,
                0, Instant.now().minus(Duration.ofHours(23)));
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

        worker.processarSincrono(envio.getId());

        assertThat(sender.chamadas).hasSize(1);
    }

    @Test
    @DisplayName("retentativa que cairia fora da janela util expira em vez de reagendar")
    void retentativaForaDaJanelaExpira() {
        AccNotificacaoEnvio envio = TestFixtures.envio(TENANT, CanalNotificacao.EMAIL, StatusEnvio.PENDENTE,
                2, Instant.now().minus(Duration.ofHours(23)).minus(Duration.ofMinutes(55)));
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));
        sender.resposta = ResultadoEnvio.transitorio("REDE", "timeout");

        worker.processarSincrono(envio.getId());

        verify(filaWriter).marcarExpirado(envio.getId());
        verify(filaWriter, never()).registrarFalha(any(), any(), any(), any());
    }

    @Test
    @DisplayName("canal sem sender registrado falha como permanente")
    void canalSemSender() {
        AccNotificacaoEnvio envio = TestFixtures.envio(TENANT, CanalNotificacao.WHATSAPP, StatusEnvio.PENDENTE,
                0, Instant.now());
        when(filaWriter.reivindicar(envio.getId())).thenReturn(Optional.of(envio));

        worker.processarSincrono(envio.getId());

        verify(filaWriter).registrarFalha(eq(envio.getId()), eq("CANAL_SEM_SENDER"), any(), isNull());
    }

    @Test
    @DisplayName("backoff segue a escala 1min, 5min, 15min, 1h e repete o ultimo degrau")
    void escalaDeBackoff() {
        assertThat(NotificacaoWorker.backoffPara(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(NotificacaoWorker.backoffPara(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(NotificacaoWorker.backoffPara(3)).isEqualTo(Duration.ofMinutes(15));
        assertThat(NotificacaoWorker.backoffPara(4)).isEqualTo(Duration.ofHours(1));
        assertThat(NotificacaoWorker.backoffPara(9)).isEqualTo(Duration.ofHours(1));
    }
}
