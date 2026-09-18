package br.com.alfaschool.backend.access.permanencia;

import br.com.alfaschool.backend.application.access.jornada.JornadaDoDia;
import br.com.alfaschool.backend.application.access.jornada.JornadaService;
import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia;
import br.com.alfaschool.backend.application.access.permanencia.EventoAcessoLeitor;
import br.com.alfaschool.backend.application.access.permanencia.EventoAcessoLeitor.EventoAcesso;
import br.com.alfaschool.backend.application.access.permanencia.PermanenciaService;
import br.com.alfaschool.backend.application.access.permanencia.PermanenciaService.ResultadoRecalculo;
import br.com.alfaschool.backend.application.access.permanencia.dto.AjusteParRequest;
import br.com.alfaschool.backend.application.access.permanencia.dto.PresencaResponse;
import br.com.alfaschool.backend.application.access.shared.CalendarioPort;
import br.com.alfaschool.backend.domain.access.permanencia.AccPresenca;
import br.com.alfaschool.backend.domain.access.permanencia.AccPresencaPar;
import br.com.alfaschool.backend.domain.access.permanencia.OrigemPar;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import br.com.alfaschool.backend.infrastructure.persistence.repository.*;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Motor de permanencia com repositorios simulados em memoria.
 *
 * Os stubs guardam estado de verdade (a presenca salva volta na proxima
 * busca) porque o que precisa ser provado aqui e' justamente o
 * comportamento entre execucoes: idempotencia, congelamento e respeito ao
 * ajuste manual.
 */
class PermanenciaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 10);   // terca-feira
    private static final LocalDate ONTEM = LocalDate.of(2026, 3, 9);

    private AccPresencaRepository presencaRepository;
    private AccPresencaParRepository parRepository;
    private AccFechamentoRepository fechamentoRepository;
    private MatriculaRepository matriculaRepository;
    private AuditLogRepository auditLogRepository;
    private JornadaService jornadaService;
    private EventoAcessoLeitor eventoLeitor;
    private CalendarioPort calendarioPort;
    private PermanenciaService service;

    private AccPresenca armazenada;
    private final List<AccPresencaPar> pares = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        presencaRepository = mock(AccPresencaRepository.class);
        parRepository = mock(AccPresencaParRepository.class);
        fechamentoRepository = mock(AccFechamentoRepository.class);
        matriculaRepository = mock(MatriculaRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        jornadaService = mock(JornadaService.class);
        eventoLeitor = mock(EventoAcessoLeitor.class);
        calendarioPort = mock(CalendarioPort.class);

        ObjectProvider<CalendarioPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(calendarioPort);
        when(calendarioPort.ehDiaLetivo(any(), any(), any())).thenReturn(true);

        Clock relogio = Clock.fixed(HOJE.atTime(14, 0).atZone(CalculoPermanencia.ZONE).toInstant(),
                CalculoPermanencia.ZONE);

        service = new PermanenciaService(presencaRepository, parRepository, fechamentoRepository,
                matriculaRepository, auditLogRepository, jornadaService, eventoLeitor, provider, relogio);

        armazenada = null;
        pares.clear();

        when(presencaRepository.findByTenantIdAndAlunoIdAndDataAndDeletedFalse(any(), any(), any()))
                .thenAnswer(i -> Optional.ofNullable(armazenada));
        when(presencaRepository.save(any(AccPresenca.class))).thenAnswer(i -> {
            AccPresenca p = i.getArgument(0);
            if (p.getId() == null) {
                atribuirId(p, UUID.randomUUID());
            }
            armazenada = p;
            return p;
        });
        when(parRepository.save(any(AccPresencaPar.class))).thenAnswer(i -> {
            AccPresencaPar p = i.getArgument(0);
            pares.add(p);
            return p;
        });
        doAnswer(i -> {
            OrigemPar origem = i.getArgument(2);
            pares.removeIf(p -> p.getOrigem() == origem);
            return null;
        }).when(parRepository).deleteByTenantIdAndPresencaIdAndOrigem(any(), any(), any());
        when(parRepository.findByTenantIdAndPresencaIdOrderByEntradaEmAsc(any(), any()))
                .thenAnswer(i -> pares.stream()
                        .sorted(Comparator.comparing(AccPresencaPar::getEntradaEm))
                        .toList());
        when(fechamentoRepository.buscarFechadosQueCobrem(any(), any())).thenReturn(List.of());
        when(jornadaService.resolverDoDia(any(), any(), any())).thenReturn(contratoPadrao());
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------ pareamento

    @Test
    void doisEventosFechamODiaComUmPar() {
        eventosDoDia(HOJE, 8, 0, 12, 0);

        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(armazenada.getStatus()).isEqualTo(StatusPresenca.FECHADA);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(240);
        assertThat(pares).hasSize(1);
        assertThat(pares.get(0).getMinutos()).isEqualTo(240);
        assertThat(pares.get(0).getOrigem()).isEqualTo(OrigemPar.EVENTO);
    }

    @Test
    void quatroEventosSomamOsDoisParesDoMesmoDia() {
        eventosDoDia(HOJE, 8, 0, 12, 0, 13, 0, 17, 0);

        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(pares).hasSize(2);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(480);
        assertThat(armazenada.getStatus()).isEqualTo(StatusPresenca.FECHADA);
    }

    @Test
    void tresEventosNoDiaCorrenteDeixamAPresencaAberta() {
        eventosDoDia(HOJE, 8, 0, 12, 0, 13, 0);

        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(armazenada.getStatus()).isEqualTo(StatusPresenca.ABERTA);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(240);
        assertThat(pares).hasSize(2);
        assertThat(pares.get(1).getSaidaEm()).isNull();
    }

    @Test
    void tresEventosOntemViramInconsistenteSemContaminarOsTotais() {
        eventosDoDia(ONTEM, 8, 0, 12, 0, 13, 0);

        service.recalcularDia(TENANT, ALUNO, ONTEM);

        assertThat(armazenada.getStatus()).isEqualTo(StatusPresenca.INCONSISTENTE);
        assertThat(armazenada.getMinutosPrevistos()).isZero();
        assertThat(armazenada.getMinutosExcedente()).isZero();
        assertThat(armazenada.getMinutosAntecipacao()).isZero();
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(240);
    }

    @Test
    void semEventoNenhumNaoCriaPresenca() {
        when(eventoLeitor.eventosDoDia(TENANT, ALUNO, HOJE)).thenReturn(List.of());

        ResultadoRecalculo r = service.reconstruir(TENANT, ALUNO, HOJE, false);

        assertThat(r).isEqualTo(ResultadoRecalculo.SEM_DADOS);
        assertThat(armazenada).isNull();
        verify(presencaRepository, never()).save(any());
    }

    // ----------------------------------------------------------- idempotencia

    @Test
    void recalcularTresVezesDaOMesmoResultado() {
        eventosDoDia(HOJE, 8, 0, 12, 0, 13, 0, 17, 0);

        service.recalcularDia(TENANT, ALUNO, HOJE);
        UUID idPrimeiro = armazenada.getId();
        int minutosPrimeiro = armazenada.getMinutosPermanencia();

        service.recalcularDia(TENANT, ALUNO, HOJE);
        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(armazenada.getId()).isEqualTo(idPrimeiro);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(minutosPrimeiro).isEqualTo(480);
        assertThat(armazenada.getStatus()).isEqualTo(StatusPresenca.FECHADA);
        // Os pares sao refeitos, nao acumulados: continua sendo dois.
        assertThat(pares).hasSize(2);
    }

    // -------------------------------------------------------------- barreiras

    @Test
    void presencaCongeladaNaoERecalculada() {
        armazenada = presencaExistente(HOJE, StatusPresenca.FECHADA, 300);
        armazenada.setCongelada(true);
        eventosDoDia(HOJE, 8, 0, 18, 0);

        ResultadoRecalculo r = service.reconstruir(TENANT, ALUNO, HOJE, false);

        assertThat(r).isEqualTo(ResultadoRecalculo.IGNORADO_CONGELADO);
        // O numero que foi para a fatura continua o mesmo.
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(300);
        verify(presencaRepository, never()).save(any());
        verify(eventoLeitor, never()).eventosDoDia(any(), any(), any());
    }

    @Test
    void diaAjustadoAMaoNaoEReconstruidoPeloJob() {
        armazenada = presencaExistente(HOJE, StatusPresenca.AJUSTADA, 300);
        eventosDoDia(HOJE, 8, 0, 18, 0);

        ResultadoRecalculo r = service.reconstruir(TENANT, ALUNO, HOJE, false);

        assertThat(r).isEqualTo(ResultadoRecalculo.IGNORADO_AJUSTADO);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(300);
    }

    @Test
    void competenciaFechadaBloqueiaORecalculoAindaQueAPresencaNaoExista() {
        eventosDoDia(ONTEM, 8, 0, 12, 0);
        when(fechamentoRepository.buscarFechadosQueCobrem(TENANT, ONTEM))
                .thenReturn(List.of(fechamentoFechado()));

        ResultadoRecalculo r = service.reconstruir(TENANT, ALUNO, ONTEM, false);

        assertThat(r).isEqualTo(ResultadoRecalculo.IGNORADO_FECHAMENTO);
        assertThat(armazenada).isNull();
    }

    // ----------------------------------------------------------- jornada e dia

    @Test
    void diaNaoLetivoZeraOPrevisto() {
        eventosDoDia(HOJE, 8, 0, 11, 0);
        when(calendarioPort.ehDiaLetivo(any(), any(), any())).thenReturn(false);

        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(armazenada.isDiaLetivo()).isFalse();
        assertThat(armazenada.getMinutosPrevistos()).isZero();
    }

    @Test
    void excecaoPontualSobrescreveOHorarioDoDia() {
        // A Ana sai as 13h por causa da consulta: previsto do dia cai de
        // 600 para 360 e sair as 15h vira excedente por HORARIO.
        eventosDoDia(HOJE, 7, 0, 15, 0);
        when(jornadaService.resolverDoDia(TENANT, ALUNO, HOJE)).thenReturn(
                new JornadaDoDia(UUID.randomUUID(), true, LocalTime.of(7, 0), LocalTime.of(13, 0),
                        360, 0, 0, RegraExcedente.AMBOS, true));

        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(armazenada.getMinutosPrevistos()).isEqualTo(360);
        assertThat(armazenada.getMinutosPermanencia()).isEqualTo(480);
        assertThat(armazenada.getMinutosExcedente()).isEqualTo(120);
    }

    @Test
    void jornadaVigenteDeCadaDiaEUsadaNaApuracao() {
        // O plano mudou no meio do periodo: 9/3 ainda vale o de 10h, 10/3
        // ja vale o de 6h. A troca nao pode reescrever o dia anterior.
        when(jornadaService.resolverDoDia(TENANT, ALUNO, ONTEM)).thenReturn(
                new JornadaDoDia(UUID.randomUUID(), true, LocalTime.of(7, 0), LocalTime.of(17, 0),
                        600, 0, 0, RegraExcedente.DURACAO, false));
        when(jornadaService.resolverDoDia(TENANT, ALUNO, HOJE)).thenReturn(
                new JornadaDoDia(UUID.randomUUID(), true, LocalTime.of(7, 0), LocalTime.of(13, 0),
                        360, 0, 0, RegraExcedente.DURACAO, false));

        eventosDoDia(ONTEM, 7, 0, 17, 0);
        service.recalcularDia(TENANT, ALUNO, ONTEM);
        int previstoOntem = armazenada.getMinutosPrevistos();
        int excedenteOntem = armazenada.getMinutosExcedente();

        armazenada = null;
        pares.clear();
        eventosDoDia(HOJE, 7, 0, 17, 0);
        service.recalcularDia(TENANT, ALUNO, HOJE);

        assertThat(previstoOntem).isEqualTo(600);
        assertThat(excedenteOntem).isZero();
        assertThat(armazenada.getMinutosPrevistos()).isEqualTo(360);
        assertThat(armazenada.getMinutosExcedente()).isEqualTo(240);
    }

    // ---------------------------------------------------------- ajuste manual

    @Test
    void ajusteManualIncluiParMarcaAjustadaERecalculaOsTotais() {
        TenantContext.setTenantId(TENANT);
        armazenada = presencaExistente(ONTEM, StatusPresenca.INCONSISTENTE, 0);

        PresencaResponse resposta = service.ajustarPar(new AjusteParRequest(
                ALUNO, ONTEM, null, em(ONTEM, 7, 0), em(ONTEM, 17, 0), false,
                "Catraca da portaria ficou offline na saida"));

        assertThat(resposta.status()).isEqualTo(StatusPresenca.AJUSTADA);
        assertThat(resposta.minutosPermanencia()).isEqualTo(600);
        assertThat(pares).hasSize(1);
        assertThat(pares.get(0).getOrigem()).isEqualTo(OrigemPar.MANUAL);
        assertThat(pares.get(0).getMotivoAjuste()).isNotBlank();
    }

    @Test
    void ajusteQueDeixaParAbertoEmDiaPassadoContinuaInconsistente() {
        TenantContext.setTenantId(TENANT);
        armazenada = presencaExistente(ONTEM, StatusPresenca.INCONSISTENTE, 0);

        PresencaResponse resposta = service.ajustarPar(new AjusteParRequest(
                ALUNO, ONTEM, null, em(ONTEM, 7, 0), null, false, "Saida ainda sera conferida"));

        // Rotular de AJUSTADA um dia ainda quebrado o faria entrar nos totais.
        assertThat(resposta.status()).isEqualTo(StatusPresenca.INCONSISTENTE);
        assertThat(resposta.contaNosTotais()).isFalse();
    }

    @Test
    void ajusteEmDiaCongeladoERecusado() {
        TenantContext.setTenantId(TENANT);
        armazenada = presencaExistente(ONTEM, StatusPresenca.FECHADA, 600);
        armazenada.setCongelada(true);

        assertThatThrownBy(() -> service.ajustarPar(new AjusteParRequest(
                ALUNO, ONTEM, null, em(ONTEM, 7, 0), em(ONTEM, 17, 0), false, "tentativa indevida")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("congelado");
    }

    @Test
    void ajusteSemTenantNoContextoFalha() {
        assertThatThrownBy(() -> service.ajustarPar(new AjusteParRequest(
                ALUNO, ONTEM, null, em(ONTEM, 7, 0), em(ONTEM, 17, 0), false, "sem tenant")))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---------------------------------------------------------------- helpers

    private void eventosDoDia(LocalDate dia, int... horaMinuto) {
        List<EventoAcesso> eventos = new ArrayList<>();
        for (int i = 0; i < horaMinuto.length; i += 2) {
            eventos.add(new EventoAcesso(UUID.randomUUID(), em(dia, horaMinuto[i], horaMinuto[i + 1]),
                    null, SentidoAcesso.INDEFINIDO));
        }
        when(eventoLeitor.eventosDoDia(TENANT, ALUNO, dia)).thenReturn(eventos);
    }

    private AccPresenca presencaExistente(LocalDate dia, StatusPresenca status, int minutos) {
        AccPresenca p = new AccPresenca();
        p.setTenantId(TENANT);
        p.setAlunoId(ALUNO);
        p.setData(dia);
        p.setStatus(status);
        p.setMinutosPermanencia(minutos);
        p.setDiaLetivo(true);
        atribuirId(p, UUID.randomUUID());
        return p;
    }

    private br.com.alfaschool.backend.domain.access.permanencia.AccFechamento fechamentoFechado() {
        var f = new br.com.alfaschool.backend.domain.access.permanencia.AccFechamento();
        f.setTenantId(TENANT);
        f.setUnitId(null);
        f.setCompetencia("2026-03");
        f.setDataInicio(LocalDate.of(2026, 3, 1));
        f.setDataFim(LocalDate.of(2026, 3, 31));
        f.setStatus(br.com.alfaschool.backend.domain.access.permanencia.AccFechamento.FECHADO);
        return f;
    }

    private static Instant em(LocalDate dia, int hora, int minuto) {
        return dia.atTime(hora, minuto).atZone(CalculoPermanencia.ZONE).toInstant();
    }

    /** Simula a geracao de id que o Hibernate faria no persist. */
    private static void atribuirId(BaseEntity entidade, UUID id) {
        try {
            Field campo = BaseEntity.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidade, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JornadaDoDia contratoPadrao() {
        return new JornadaDoDia(UUID.randomUUID(), true, LocalTime.of(7, 0), LocalTime.of(17, 0),
                600, 0, 0, RegraExcedente.DURACAO, false);
    }
}
