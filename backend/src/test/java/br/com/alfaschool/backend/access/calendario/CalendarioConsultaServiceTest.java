package br.com.alfaschool.backend.access.calendario;

import br.com.alfaschool.backend.application.access.calendario.CalendarioConsultaService;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendarioDia;
import br.com.alfaschool.backend.domain.access.shared.TipoCalendarioDia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioConsultaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID UNIDADE = UUID.randomUUID();
    private static final int ANO = 2026;
    private static final LocalDate PRIMEIRO = LocalDate.of(ANO, 1, 1);
    private static final LocalDate ULTIMO = LocalDate.of(ANO, 12, 31);

    @Mock
    private AccCalendarioRepository calendarioRepository;

    @Mock
    private AccCalendarioDiaRepository diaRepository;

    private CalendarioConsultaService service;

    private AccCalendario global;
    private AccCalendario daUnidade;

    @BeforeEach
    void setUp() {
        service = new CalendarioConsultaService(calendarioRepository, diaRepository);
        global = calendario(null);
        daUnidade = calendario(UNIDADE);
    }

    // ------------------------------------------------------------------
    // Dia cadastrado: o tipo decide
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dia cadastrado: LETIVO, SABADO_LETIVO e EVENTO sao letivos")
    void diasCadastradosQueSaoLetivos() {
        LocalDate letivo = LocalDate.of(ANO, 3, 11);        // quarta
        LocalDate sabadoLetivo = LocalDate.of(ANO, 3, 14);  // sabado
        LocalDate evento = LocalDate.of(ANO, 6, 13);        // sabado, mostra pedagogica

        comDiasGlobais(
                dia(letivo, TipoCalendarioDia.LETIVO),
                dia(sabadoLetivo, TipoCalendarioDia.SABADO_LETIVO),
                dia(evento, TipoCalendarioDia.EVENTO));

        assertThat(service.ehDiaLetivo(TENANT, null, letivo)).isTrue();
        // Sabado so vira letivo porque foi lancado: sem lancamento cairia no padrao.
        assertThat(sabadoLetivo.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(service.ehDiaLetivo(TENANT, null, sabadoLetivo)).isTrue();
        assertThat(service.ehDiaLetivo(TENANT, null, evento)).isTrue();
    }

    @Test
    @DisplayName("dia cadastrado: FERIADO, RECESSO e FACULTATIVO nao sao letivos")
    void diasCadastradosQueNaoSaoLetivos() {
        LocalDate feriado = LocalDate.of(ANO, 4, 21);      // terca, Tiradentes
        LocalDate recesso = LocalDate.of(ANO, 12, 25);     // sexta
        LocalDate facultativo = LocalDate.of(ANO, 5, 20);  // quarta

        comDiasGlobais(
                dia(feriado, TipoCalendarioDia.FERIADO),
                dia(recesso, TipoCalendarioDia.RECESSO),
                dia(facultativo, TipoCalendarioDia.FACULTATIVO));

        assertThat(service.ehDiaLetivo(TENANT, null, feriado)).isFalse();
        assertThat(service.ehDiaLetivo(TENANT, null, recesso)).isFalse();
        // Dia util lancado como facultativo: a escola pode abrir, mas nao cobra presenca.
        assertThat(facultativo.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(service.ehDiaLetivo(TENANT, null, facultativo)).isFalse();
    }

    // ------------------------------------------------------------------
    // Dia nao cadastrado: cai no padrao da semana
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dia nao cadastrado em dia util e letivo")
    void diaNaoCadastradoEmDiaUtil() {
        LocalDate quarta = LocalDate.of(ANO, 9, 16);
        comDiasGlobais();

        assertThat(quarta.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(service.ehDiaLetivo(TENANT, null, quarta)).isTrue();
    }

    @Test
    @DisplayName("dia nao cadastrado em domingo nao e letivo")
    void diaNaoCadastradoEmDomingo() {
        LocalDate domingo = LocalDate.of(ANO, 3, 15);
        comDiasGlobais();

        assertThat(domingo.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(service.ehDiaLetivo(TENANT, null, domingo)).isFalse();
    }

    @Test
    @DisplayName("sem nenhum calendario cadastrado, vale o padrao da semana")
    void semCalendarioNenhum() {
        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of());

        assertThat(service.ehDiaLetivo(TENANT, null, LocalDate.of(ANO, 10, 12))).isTrue();   // segunda
        assertThat(service.ehDiaLetivo(TENANT, null, LocalDate.of(ANO, 3, 15))).isFalse();   // domingo
        verify(diaRepository, never()).findByCalendarioIdInAndDataBetweenAndDeletedFalse(any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Precedencia unidade sobre global
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o calendario da unidade sobrepoe o global na mesma data")
    void unidadeSobrepoeGlobal() {
        LocalDate data = LocalDate.of(ANO, 11, 20); // sexta

        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of(global));
        when(calendarioRepository.ativosDaUnidade(TENANT, UNIDADE, ANO)).thenReturn(List.of(daUnidade));
        when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                eq(List.of(global.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                .thenReturn(List.of(dia(data, TipoCalendarioDia.LETIVO)));
        when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                eq(List.of(daUnidade.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                .thenReturn(List.of(dia(data, TipoCalendarioDia.FERIADO)));

        // A unidade decretou feriado local no que a rede trata como dia letivo.
        assertThat(service.ehDiaLetivo(TENANT, UNIDADE, data)).isFalse();
        // O escopo global segue enxergando o proprio lancamento.
        assertThat(service.ehDiaLetivo(TENANT, null, data)).isTrue();
    }

    @Test
    @DisplayName("sem calendario da unidade, o global vale para a unidade")
    void semCalendarioDaUnidadeCaiNoGlobal() {
        LocalDate data = LocalDate.of(ANO, 11, 20);

        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of(global));
        when(calendarioRepository.ativosDaUnidade(TENANT, UNIDADE, ANO)).thenReturn(List.of());
        when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                eq(List.of(global.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                .thenReturn(List.of(dia(data, TipoCalendarioDia.FERIADO)));

        assertThat(service.ehDiaLetivo(TENANT, UNIDADE, data)).isFalse();
    }

    @Test
    @DisplayName("data nao lancada pela unidade herda o lancamento global")
    void unidadeHerdaDataNaoLancada() {
        LocalDate feriadoNacional = LocalDate.of(ANO, 4, 21);
        LocalDate eventoLocal = LocalDate.of(ANO, 6, 13);

        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of(global));
        when(calendarioRepository.ativosDaUnidade(TENANT, UNIDADE, ANO)).thenReturn(List.of(daUnidade));
        when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                eq(List.of(global.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                .thenReturn(List.of(dia(feriadoNacional, TipoCalendarioDia.FERIADO)));
        when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                eq(List.of(daUnidade.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                .thenReturn(List.of(dia(eventoLocal, TipoCalendarioDia.EVENTO)));

        // A unidade nao repete os feriados da rede; ela so acrescenta o que e' seu.
        assertThat(service.ehDiaLetivo(TENANT, UNIDADE, feriadoNacional)).isFalse();
        assertThat(service.ehDiaLetivo(TENANT, UNIDADE, eventoLocal)).isTrue();
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o ano e carregado uma vez so; invalidar forca a releitura")
    void cachePorAnoEInvalidacao() {
        comDiasGlobais(dia(LocalDate.of(ANO, 4, 21), TipoCalendarioDia.FERIADO));

        for (int i = 0; i < 50; i++) {
            service.ehDiaLetivo(TENANT, null, LocalDate.of(ANO, 4, 21));
        }
        verify(calendarioRepository, times(1)).ativosGlobais(TENANT, ANO);

        service.invalidar(TENANT, null, ANO);
        service.ehDiaLetivo(TENANT, null, LocalDate.of(ANO, 4, 21));
        verify(calendarioRepository, times(2)).ativosGlobais(TENANT, ANO);
    }

    @Test
    @DisplayName("invalidar o calendario global derruba tambem os recortes por unidade")
    void invalidarGlobalDerrubaUnidades() {
        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of());
        when(calendarioRepository.ativosDaUnidade(TENANT, UNIDADE, ANO)).thenReturn(List.of());

        service.ehDiaLetivo(TENANT, UNIDADE, LocalDate.of(ANO, 9, 16));
        service.ehDiaLetivo(TENANT, null, LocalDate.of(ANO, 9, 16));
        assertThat(service.tamanhoCache()).isEqualTo(2);

        service.invalidar(TENANT, null, ANO);

        assertThat(service.tamanhoCache()).isZero();
        assertThat(service.contemRecorte(TENANT, UNIDADE, ANO)).isFalse();
    }

    // ------------------------------------------------------------------

    private void comDiasGlobais(AccCalendarioDia... dias) {
        when(calendarioRepository.ativosGlobais(TENANT, ANO)).thenReturn(List.of(global));
        if (dias.length > 0) {
            when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                    eq(List.of(global.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                    .thenReturn(List.of(dias));
        } else {
            when(diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                    eq(List.of(global.getId())), eq(PRIMEIRO), eq(ULTIMO)))
                    .thenReturn(List.of());
        }
    }

    private AccCalendario calendario(UUID unitId) {
        AccCalendario calendario = new AccCalendario();
        ReflectionTestUtils.setField(calendario, "id", UUID.randomUUID());
        calendario.setTenantId(TENANT);
        calendario.setUnitId(unitId);
        calendario.setAnoLetivo(ANO);
        calendario.setNome(unitId == null ? "Rede " + ANO : "Unidade " + ANO);
        calendario.setAtivo(true);
        return calendario;
    }

    private AccCalendarioDia dia(LocalDate data, TipoCalendarioDia tipo) {
        AccCalendarioDia dia = new AccCalendarioDia();
        ReflectionTestUtils.setField(dia, "id", UUID.randomUUID());
        dia.setTenantId(TENANT);
        dia.setData(data);
        dia.setTipo(tipo);
        return dia;
    }
}
