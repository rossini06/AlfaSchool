package br.com.alfaschool.backend.access.evento;

import br.com.alfaschool.backend.application.access.evento.EventoIngestaoService;
import br.com.alfaschool.backend.application.access.evento.RelogioEquipamento;
import br.com.alfaschool.backend.application.access.evento.dto.LeituraBruta;
import br.com.alfaschool.backend.application.access.evento.dto.ResultadoIngestao;
import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.evento.AccEvento;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccEventoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ingestao de eventos: deduplicacao, tempo e resolucao de titular.
 *
 * O repositorio e' simulado com armazenamento em memoria que reproduz a
 * mesma consulta da JPQL (mesmo dispositivo + mesmo device_log_id dentro
 * da janela). Assim o teste avalia a REGRA, e nao apenas se o service
 * chamou o mock.
 */
class EventoIngestaoServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private AccEventoRepository eventos;
    private AccFaceRepository faces;
    private DispositivoRepository dispositivos;
    private ApplicationEventPublisher publisher;
    private EventoIngestaoService service;

    private final List<AccEvento> armazenados = new ArrayList<>();

    private UUID tenantId;
    private UUID dispositivoId;
    private Dispositivo dispositivo;

    @BeforeEach
    void preparar() {
        eventos = mock(AccEventoRepository.class);
        faces = mock(AccFaceRepository.class);
        dispositivos = mock(DispositivoRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        armazenados.clear();

        tenantId = UUID.randomUUID();
        dispositivoId = UUID.randomUUID();

        dispositivo = new Dispositivo();
        ReflectionTestUtils.setField(dispositivo, "id", dispositivoId);
        dispositivo.setTenantId(tenantId);
        dispositivo.setDeleted(false);
        dispositivo.setNome("Catraca Entrada");
        dispositivo.setUnitId(UUID.randomUUID());
        dispositivo.setFuncao(FuncaoDispositivo.ALUNO);
        dispositivo.setSentido(SentidoAcesso.ENTRADA);

        when(dispositivos.findById(dispositivoId)).thenReturn(Optional.of(dispositivo));

        // Reproduz a JPQL buscarReplay sobre a lista em memoria.
        when(eventos.buscarReplay(any(), any(), any(), any())).thenAnswer(inv -> {
            UUID disp = inv.getArgument(0);
            Long logId = inv.getArgument(1);
            Instant inicio = inv.getArgument(2);
            Instant fim = inv.getArgument(3);
            return armazenados.stream()
                    .filter(e -> disp.equals(e.getDispositivoId()))
                    .filter(e -> logId.equals(e.getDeviceLogId()))
                    .filter(e -> !e.getDataHora().isBefore(inicio) && !e.getDataHora().isAfter(fim))
                    .toList();
        });
        when(eventos.save(any())).thenAnswer(inv -> {
            AccEvento e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            armazenados.add(e);
            return e;
        });
        when(faces.findByTenantIdAndDeviceUserIdAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        service = new EventoIngestaoService(eventos, faces, dispositivos, publisher,
                "America/Sao_Paulo", 60);
    }

    private LeituraBruta leitura(long deviceLogId, long deviceUserId, Instant quando) {
        return new LeituraBruta(dispositivoId, deviceLogId, deviceUserId,
                RelogioEquipamento.paraEpochLocal(quando, SP), null, 7,
                null, TipoIdentificacao.FACE, null, null, OrigemEvento.AGENTE, "{}");
    }

    // =================================================================
    // Deduplicacao
    // =================================================================

    @Test
    @DisplayName("mesmo device_log_id dentro da janela e' replay: nao grava evento novo")
    void mesmoLogIdDentroDaJanelaEhReplay() {
        Instant momento = Instant.now().minusSeconds(120);

        ResultadoIngestao primeiro = service.registrar(tenantId, leitura(500L, 1001L, momento));
        ResultadoIngestao segundo = service.registrar(tenantId,
                leitura(500L, 1001L, momento.plusSeconds(10)));

        assertThat(primeiro.replay()).isFalse();
        assertThat(segundo.replay()).isTrue();
        assertThat(segundo.eventoId()).isEqualTo(primeiro.eventoId());
        assertThat(armazenados).hasSize(1);
        // Replay nao publica: notificar a familia duas vezes da mesma
        // entrada e' o efeito colateral que a dedup existe para evitar.
        verify(publisher, times(1)).publishEvent(any(AcessoRegistradoEvent.class));
    }

    @Test
    @DisplayName("mesmo device_log_id fora da janela e' evento novo")
    void mesmoLogIdForaDaJanelaEhEventoNovo() {
        Instant momento = Instant.now().minusSeconds(7200);

        ResultadoIngestao primeiro = service.registrar(tenantId, leitura(500L, 1001L, momento));
        ResultadoIngestao segundo = service.registrar(tenantId,
                leitura(500L, 1001L, momento.plusSeconds(3600)));

        assertThat(primeiro.replay()).isFalse();
        assertThat(segundo.replay()).isFalse();
        assertThat(segundo.eventoId()).isNotEqualTo(primeiro.eventoId());
        assertThat(armazenados).hasSize(2);
    }

    @Test
    @DisplayName("contador reiniciado no leitor: device_log_id MENOR que o ultimo nao e' descartado")
    void contadorReiniciadoNaoPerdeEvento() {
        Instant ontem = Instant.now().minusSeconds(86_400);
        service.registrar(tenantId, leitura(9001L, 1001L, ontem));
        service.registrar(tenantId, leitura(9002L, 1002L, ontem.plusSeconds(60)));

        // Alguem limpou o historico do equipamento: o contador voltou a 1.
        ResultadoIngestao depoisDaLimpeza =
                service.registrar(tenantId, leitura(1L, 1003L, Instant.now()));

        assertThat(depoisDaLimpeza.replay()).isFalse();
        assertThat(armazenados).hasSize(3);
    }

    @Test
    @DisplayName("device_log_id repetido apos limpeza, longe no tempo, gera evento novo")
    void logIdRepetidoAposLimpezaNaoViraBuracoNegro() {
        Instant anoPassado = Instant.now().minusSeconds(86_400L * 200);
        ResultadoIngestao antigo = service.registrar(tenantId, leitura(1L, 1001L, anoPassado));

        // Depois da limpeza o equipamento emite o id 1 de novo, hoje.
        ResultadoIngestao novo = service.registrar(tenantId, leitura(1L, 1002L, Instant.now()));

        assertThat(novo.replay()).isFalse();
        assertThat(novo.eventoId()).isNotEqualTo(antigo.eventoId());
    }

    @Test
    @DisplayName("sem device_log_id nao ha deduplicacao: duplicar e' melhor que perder")
    void semLogIdNaoDeduplica() {
        LeituraBruta semId = new LeituraBruta(dispositivoId, null, 1001L,
                RelogioEquipamento.paraEpochLocal(Instant.now(), SP), null, 7,
                null, TipoIdentificacao.FACE, null, null, OrigemEvento.WEBHOOK, "{}");

        service.registrar(tenantId, semId);
        ResultadoIngestao segundo = service.registrar(tenantId, semId);

        assertThat(segundo.replay()).isFalse();
        assertThat(armazenados).hasSize(2);
        verify(eventos, never()).buscarReplay(any(), eq(null), any(), any());
    }

    // =================================================================
    // Titular
    // =================================================================

    @Test
    @DisplayName("device_user_id sem face vira DESCONHECIDO/NULL — nunca inventa id")
    void titularDesconhecidoNaoInventaId() {
        service.registrar(tenantId, leitura(10L, 4242L, Instant.now()));

        AccEvento gravado = armazenados.get(0);
        assertThat(gravado.getTitularTipo()).isEqualTo(TitularTipo.DESCONHECIDO);
        assertThat(gravado.getTitularId()).isNull();
        // O numero do equipamento fica onde e' verdade: em device_user_id.
        assertThat(gravado.getDeviceUserId()).isEqualTo(4242L);
    }

    @Test
    @DisplayName("user_id 0 (nao identificado pelo leitor) grava DESCONHECIDO")
    void usuarioZeroEhDesconhecido() {
        service.registrar(tenantId, leitura(11L, 0L, Instant.now()));

        AccEvento gravado = armazenados.get(0);
        assertThat(gravado.getTitularTipo()).isEqualTo(TitularTipo.DESCONHECIDO);
        assertThat(gravado.getTitularId()).isNull();
        verify(faces, never()).findByTenantIdAndDeviceUserIdAndDeletedFalse(any(), eq(0L));
    }

    @Test
    @DisplayName("device_user_id com face cadastrada resolve o titular real")
    void titularResolvidoPelaFace() {
        UUID alunoId = UUID.randomUUID();
        AccFace face = new AccFace();
        face.setTenantId(tenantId);
        face.setTitularTipo(TitularTipo.ALUNO);
        face.setTitularId(alunoId);
        face.setDeviceUserId(1001L);
        when(faces.findByTenantIdAndDeviceUserIdAndDeletedFalse(tenantId, 1001L))
                .thenReturn(Optional.of(face));

        service.registrar(tenantId, leitura(12L, 1001L, Instant.now()));

        AccEvento gravado = armazenados.get(0);
        assertThat(gravado.getTitularTipo()).isEqualTo(TitularTipo.ALUNO);
        assertThat(gravado.getTitularId()).isEqualTo(alunoId);
    }

    // =================================================================
    // Tempo
    // =================================================================

    @Test
    @DisplayName("relogio do leitor adiantado: data_hora e' grampeada na hora do servidor")
    void futuroEhGrampeado() {
        Instant antes = Instant.now();
        service.registrar(tenantId, leitura(20L, 1001L, antes.plusSeconds(3600)));
        Instant depois = Instant.now();

        AccEvento gravado = armazenados.get(0);
        assertThat(gravado.getDataHora()).isBetween(antes, depois);
    }

    @Test
    @DisplayName("evento antigo da fila offline e' aceito com a hora original")
    void passadoEhAceitoComHoraOriginal() {
        Instant ontem = Instant.now().minusSeconds(86_400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        service.registrar(tenantId, leitura(21L, 1001L, ontem));

        assertThat(armazenados.get(0).getDataHora()).isEqualTo(ontem);
        // recebido_em guarda quando chegou; data_hora guarda quando aconteceu.
        assertThat(armazenados.get(0).getRecebidoEm()).isAfter(ontem);
    }

    @Test
    @DisplayName("epoch do firmware e' lido no fuso do equipamento, nao como UTC")
    void epochDoFirmwareUsaFusoDoEquipamento() {
        Instant momento = Instant.parse("2026-05-10T10:20:00Z");

        service.registrar(tenantId, leitura(22L, 1001L, momento));

        assertThat(armazenados.get(0).getDataHora()).isEqualTo(momento);
    }

    // =================================================================
    // Publicacao
    // =================================================================

    @Test
    @DisplayName("evento publicado carrega funcao do leitor, sentido e resultado")
    void publicaEventoDeIntegracao() {
        service.registrar(tenantId, leitura(30L, 1001L, Instant.now()));

        ArgumentCaptor<AcessoRegistradoEvent> captor =
                ArgumentCaptor.forClass(AcessoRegistradoEvent.class);
        verify(publisher).publishEvent(captor.capture());

        AcessoRegistradoEvent publicado = captor.getValue();
        assertThat(publicado.tenantId()).isEqualTo(tenantId);
        assertThat(publicado.dispositivoId()).isEqualTo(dispositivoId);
        assertThat(publicado.funcaoDispositivo()).isEqualTo(FuncaoDispositivo.ALUNO);
        assertThat(publicado.sentido()).isEqualTo(SentidoAcesso.ENTRADA);
        assertThat(publicado.resultado()).isEqualTo(ResultadoAcesso.PERMITIDO);
        assertThat(publicado.permitido()).isTrue();
    }

    @Test
    @DisplayName("codigo de evento do firmware vira resultado; sem codigo, DESCONHECIDO")
    void traducaoDoEventCode() {
        assertThat(EventoIngestaoService.resultadoDoEventCode(7))
                .isEqualTo(ResultadoAcesso.PERMITIDO);
        assertThat(EventoIngestaoService.resultadoDoEventCode(6))
                .isEqualTo(ResultadoAcesso.NEGADO);
        assertThat(EventoIngestaoService.resultadoDoEventCode(8))
                .isEqualTo(ResultadoAcesso.NEGADO);
        // Presumir PERMITIDO encheria a presenca de entradas que nunca
        // aconteceram; o padrao seguro e' admitir que nao se sabe.
        assertThat(EventoIngestaoService.resultadoDoEventCode(null))
                .isEqualTo(ResultadoAcesso.DESCONHECIDO);
        assertThat(EventoIngestaoService.resultadoDoEventCode(99))
                .isEqualTo(ResultadoAcesso.DESCONHECIDO);
    }

    @Test
    @DisplayName("evento de outro tenant no mesmo dispositivo e' rejeitado")
    void tenantErradoNaoPassa() {
        UUID outroTenant = UUID.randomUUID();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.registrar(outroTenant, leitura(40L, 1001L, Instant.now())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Equipamento não encontrado");
    }
}
