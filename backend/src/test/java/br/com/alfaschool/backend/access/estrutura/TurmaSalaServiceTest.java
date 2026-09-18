package br.com.alfaschool.backend.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.TurmaSalaService;
import br.com.alfaschool.backend.application.access.estrutura.dto.TurmaSalaRequest;
import br.com.alfaschool.backend.domain.access.estrutura.AccSala;
import br.com.alfaschool.backend.domain.access.estrutura.AccTurmaSala;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccSalaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccTurmaSalaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurmaSalaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID UNIDADE = UUID.randomUUID();
    private static final UUID TURMA = UUID.randomUUID();
    private static final UUID SALA_FIXA = UUID.randomUUID();
    private static final UUID LABORATORIO = UUID.randomUUID();

    private static final LocalDate INICIO_ANO = LocalDate.of(2026, 2, 2);
    private static final LocalDate FIM_ANO = LocalDate.of(2026, 12, 18);

    @Mock
    private AccTurmaSalaRepository turmaSalaRepository;

    @Mock
    private AccSalaRepository salaRepository;

    private TurmaSalaService service;

    @BeforeEach
    void setUp() {
        service = new TurmaSalaService(turmaSalaRepository, salaRepository);
        TenantContext.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // salaVigenteDaTurma
    // ------------------------------------------------------------------

    @Test
    @DisplayName("vigencia expirada nao resolve sala")
    void vigenciaExpirada() {
        AccTurmaSala encerrado = vinculo(SALA_FIXA, INICIO_ANO, LocalDate.of(2026, 6, 30), null, null, null);
        LocalDateTime depois = LocalDateTime.of(2026, 9, 16, 10, 0); // quarta

        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, depois.toLocalDate()))
                .thenReturn(List.of(encerrado));

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, depois)).isEmpty();
    }

    @Test
    @DisplayName("vinculo que ainda nao comecou nao resolve sala")
    void vigenciaFutura() {
        AccTurmaSala futuro = vinculo(SALA_FIXA, LocalDate.of(2026, 8, 1), null, null, null, null);
        LocalDateTime antes = LocalDateTime.of(2026, 3, 11, 10, 0);

        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, antes.toLocalDate()))
                .thenReturn(List.of(futuro));

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, antes)).isEmpty();
    }

    @Test
    @DisplayName("dia da semana que nao bate nao resolve sala")
    void diaDaSemanaNaoBate() {
        AccTurmaSala apenasDiasUteis = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO, null, null, "1,2,3,4,5");
        LocalDateTime sabado = LocalDateTime.of(2026, 3, 14, 10, 0);

        assertThat(sabado.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, sabado.toLocalDate()))
                .thenReturn(List.of(apenasDiasUteis));

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, sabado)).isEmpty();
    }

    @Test
    @DisplayName("faixa de horario que nao bate nao resolve sala")
    void faixaDeHorarioNaoBate() {
        AccTurmaSala tarde = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO,
                LocalTime.of(13, 0), LocalTime.of(18, 0), "1,2,3,4,5");
        LocalDateTime deManha = LocalDateTime.of(2026, 3, 11, 10, 0); // quarta

        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, deManha.toLocalDate()))
                .thenReturn(List.of(tarde));

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, deManha)).isEmpty();
    }

    @Test
    @DisplayName("fim da faixa e exclusivo: turnos encostados nao se sobrepoem na virada")
    void fimDaFaixaEExclusivo() {
        AccTurmaSala manha = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO,
                LocalTime.of(8, 0), LocalTime.of(12, 0), null);

        assertThat(manha.aplicaEm(LocalDateTime.of(2026, 3, 11, 8, 0))).isTrue();
        assertThat(manha.aplicaEm(LocalDateTime.of(2026, 3, 11, 11, 59))).isTrue();
        assertThat(manha.aplicaEm(LocalDateTime.of(2026, 3, 11, 12, 0))).isFalse();
    }

    @Test
    @DisplayName("desempate por especificidade: a faixa de horario ganha do vinculo aberto")
    void desempatePorEspecificidade() {
        AccTurmaSala salaFixa = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO, null, null, "1,2,3,4,5");
        AccTurmaSala aulaNoLaboratorio = vinculo(LABORATORIO, INICIO_ANO, FIM_ANO,
                LocalTime.of(10, 0), LocalTime.of(12, 0), "3");

        LocalDateTime duranteOLaboratorio = LocalDateTime.of(2026, 3, 11, 10, 30); // quarta
        LocalDateTime foraDoLaboratorio = LocalDateTime.of(2026, 3, 11, 14, 0);

        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, LocalDate.of(2026, 3, 11)))
                .thenReturn(List.of(salaFixa, aulaNoLaboratorio));

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, duranteOLaboratorio))
                .get()
                .extracting(AccTurmaSala::getSalaId)
                .isEqualTo(LABORATORIO);

        assertThat(service.resolverVinculoVigente(TENANT, TURMA, foraDoLaboratorio))
                .get()
                .extracting(AccTurmaSala::getSalaId)
                .isEqualTo(SALA_FIXA);
    }

    @Test
    @DisplayName("salaVigenteDaTurma devolve a sala e o vinculo que a explica")
    void salaVigenteComSala() {
        AccTurmaSala salaFixa = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO, null, null, "1,2,3,4,5");
        LocalDateTime momento = LocalDateTime.of(2026, 3, 11, 9, 0);

        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, momento.toLocalDate()))
                .thenReturn(List.of(salaFixa));
        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(SALA_FIXA, TENANT))
                .thenReturn(Optional.of(sala(SALA_FIXA, "1o Ano A")));

        var resposta = service.salaVigenteDaTurma(TURMA, momento);

        assertThat(resposta.sala().id()).isEqualTo(SALA_FIXA);
        assertThat(resposta.vinculo().salaId()).isEqualTo(SALA_FIXA);
    }

    @Test
    @DisplayName("sem vinculo vigente, salaVigenteDaTurma devolve 404")
    void salaVigenteSemVinculo() {
        LocalDateTime domingo = LocalDateTime.of(2026, 3, 15, 10, 0);
        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, domingo.toLocalDate()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.salaVigenteDaTurma(TURMA, domingo))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ------------------------------------------------------------------
    // turmasNaSala
    // ------------------------------------------------------------------

    @Test
    @DisplayName("turmasNaSala nao mostra a turma que esta no laboratorio naquele horario")
    void turmasNaSalaRespeitaODesempate() {
        AccTurmaSala salaFixa = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO, null, null, "1,2,3,4,5");
        AccTurmaSala aulaNoLaboratorio = vinculo(LABORATORIO, INICIO_ANO, FIM_ANO,
                LocalTime.of(10, 0), LocalTime.of(12, 0), "3");
        LocalDateTime momento = LocalDateTime.of(2026, 3, 11, 10, 30);

        when(turmaSalaRepository.candidatosPorSala(TENANT, SALA_FIXA, momento.toLocalDate()))
                .thenReturn(List.of(salaFixa));
        when(turmaSalaRepository.candidatosPorSala(TENANT, LABORATORIO, momento.toLocalDate()))
                .thenReturn(List.of(aulaNoLaboratorio));
        when(turmaSalaRepository.candidatosPorTurma(TENANT, TURMA, momento.toLocalDate()))
                .thenReturn(List.of(salaFixa, aulaNoLaboratorio));

        assertThat(service.turmasNaSala(SALA_FIXA, momento)).isEmpty();
        assertThat(service.turmasNaSala(LABORATORIO, momento))
                .singleElement()
                .extracting(v -> v.turmaId())
                .isEqualTo(TURMA);
    }

    // ------------------------------------------------------------------
    // Sobreposicao
    // ------------------------------------------------------------------

    @Test
    @DisplayName("vinculos sobrepostos da mesma turma sao rejeitados com 409")
    void sobreposicaoRejeitada() {
        AccTurmaSala existente = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO,
                LocalTime.of(8, 0), LocalTime.of(12, 0), "1,2,3,4,5");
        // Mesma turma, outra sala, mesmo periodo, mesma quarta-feira, horario cruzado.
        TurmaSalaRequest conflitante = new TurmaSalaRequest(TURMA, LABORATORIO,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30),
                LocalTime.of(11, 0), LocalTime.of(13, 0), "3");

        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(LABORATORIO, TENANT))
                .thenReturn(Optional.of(sala(LABORATORIO, "Laboratorio")));
        when(turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(TENANT, TURMA))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.create(conflitante))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException erro = (ResponseStatusException) e;
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).contains("Conflito de vinculo turma-sala");
                });
    }

    @Test
    @DisplayName("horarios encostados nao sao sobreposicao")
    void horariosEncostadosPassam() {
        AccTurmaSala manha = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO,
                LocalTime.of(8, 0), LocalTime.of(12, 0), "1,2,3,4,5");
        TurmaSalaRequest tarde = new TurmaSalaRequest(TURMA, LABORATORIO, INICIO_ANO, FIM_ANO,
                LocalTime.of(12, 0), LocalTime.of(18, 0), "1,2,3,4,5");

        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(LABORATORIO, TENANT))
                .thenReturn(Optional.of(sala(LABORATORIO, "Laboratorio")));
        when(turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(TENANT, TURMA))
                .thenReturn(List.of(manha));
        when(turmaSalaRepository.save(any(AccTurmaSala.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(tarde).horaInicio()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("dias da semana disjuntos nao sao sobreposicao")
    void diasDisjuntosPassam() {
        AccTurmaSala segundaEQuarta = vinculo(SALA_FIXA, INICIO_ANO, FIM_ANO,
                LocalTime.of(8, 0), LocalTime.of(12, 0), "1,3");
        TurmaSalaRequest tercaEQuinta = new TurmaSalaRequest(TURMA, LABORATORIO, INICIO_ANO, FIM_ANO,
                LocalTime.of(8, 0), LocalTime.of(12, 0), "2,4");

        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(LABORATORIO, TENANT))
                .thenReturn(Optional.of(sala(LABORATORIO, "Laboratorio")));
        when(turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(TENANT, TURMA))
                .thenReturn(List.of(segundaEQuarta));
        when(turmaSalaRepository.save(any(AccTurmaSala.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(tercaEQuinta).diasSemana()).isEqualTo("2,4");
    }

    @Test
    @DisplayName("vigencias que nao se cruzam nao sao sobreposicao")
    void vigenciasDisjuntasPassam() {
        AccTurmaSala primeiroSemestre = vinculo(SALA_FIXA, INICIO_ANO, LocalDate.of(2026, 6, 30), null, null, null);
        TurmaSalaRequest segundoSemestre = new TurmaSalaRequest(TURMA, LABORATORIO,
                LocalDate.of(2026, 7, 1), FIM_ANO, null, null, null);

        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(LABORATORIO, TENANT))
                .thenReturn(Optional.of(sala(LABORATORIO, "Laboratorio")));
        when(turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(TENANT, TURMA))
                .thenReturn(List.of(primeiroSemestre));
        when(turmaSalaRepository.save(any(AccTurmaSala.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(segundoSemestre).vigenciaInicio()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("vigencia aberta cruza qualquer periodo posterior")
    void vigenciaAbertaCruza() {
        AccTurmaSala semFim = vinculo(SALA_FIXA, INICIO_ANO, null, null, null, null);
        TurmaSalaRequest depois = new TurmaSalaRequest(TURMA, LABORATORIO,
                LocalDate.of(2027, 3, 1), null, null, null, null);

        when(salaRepository.findByIdAndTenantIdAndDeletedFalse(LABORATORIO, TENANT))
                .thenReturn(Optional.of(sala(LABORATORIO, "Laboratorio")));
        when(turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(TENANT, TURMA))
                .thenReturn(List.of(semFim));

        assertThatThrownBy(() -> service.create(depois))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("horaFim menor ou igual a horaInicio e rejeitada com 400")
    void faixaHorariaInvertida() {
        TurmaSalaRequest invalido = new TurmaSalaRequest(TURMA, LABORATORIO, INICIO_ANO, FIM_ANO,
                LocalTime.of(12, 0), LocalTime.of(8, 0), null);

        assertThatThrownBy(() -> service.create(invalido))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ------------------------------------------------------------------

    private AccTurmaSala vinculo(UUID salaId, LocalDate inicio, LocalDate fim,
                                 LocalTime horaInicio, LocalTime horaFim, String diasSemana) {
        AccTurmaSala vinculo = new AccTurmaSala();
        ReflectionTestUtils.setField(vinculo, "id", UUID.randomUUID());
        vinculo.setTenantId(TENANT);
        vinculo.setTurmaId(TURMA);
        vinculo.setSalaId(salaId);
        vinculo.setVigenciaInicio(inicio);
        vinculo.setVigenciaFim(fim);
        vinculo.setHoraInicio(horaInicio);
        vinculo.setHoraFim(horaFim);
        vinculo.setDiasSemana(diasSemana);
        return vinculo;
    }

    private AccSala sala(UUID id, String nome) {
        AccSala sala = new AccSala();
        ReflectionTestUtils.setField(sala, "id", id);
        sala.setTenantId(TENANT);
        sala.setUnitId(UNIDADE);
        sala.setNome(nome);
        return sala;
    }
}
