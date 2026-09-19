package br.com.alfaschool.backend.access.jornada;

import br.com.alfaschool.backend.application.access.jornada.JornadaDoDia;
import br.com.alfaschool.backend.application.access.jornada.JornadaService;
import br.com.alfaschool.backend.application.access.jornada.dto.AlunoJornadaRequest;
import br.com.alfaschool.backend.application.access.jornada.dto.AplicarJornadaRequest;
import br.com.alfaschool.backend.domain.access.jornada.AccAlunoJornada;
import br.com.alfaschool.backend.domain.access.jornada.AccJornada;
import br.com.alfaschool.backend.domain.access.jornada.AccJornadaDia;
import br.com.alfaschool.backend.domain.access.jornada.AccJornadaExcecao;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAlunoJornadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaExcecaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Resolucao do contratado por dia — a entrada do motor de apuracao.
 *
 * O caso que mais importa aqui e' a vigencia: trocar o plano em agosto
 * nao pode mudar o que a familia pagou em marco.
 */
class JornadaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final LocalDate MARCO = LocalDate.of(2026, 3, 10);   // terca
    private static final LocalDate AGOSTO = LocalDate.of(2026, 8, 11);  // terca

    private AccJornadaRepository jornadaRepository;
    private AccJornadaDiaRepository jornadaDiaRepository;
    private AccAlunoJornadaRepository alunoJornadaRepository;
    private AccJornadaExcecaoRepository excecaoRepository;
    private JornadaService service;

    @BeforeEach
    void setUp() {
        jornadaRepository = mock(AccJornadaRepository.class);
        jornadaDiaRepository = mock(AccJornadaDiaRepository.class);
        alunoJornadaRepository = mock(AccAlunoJornadaRepository.class);
        excecaoRepository = mock(AccJornadaExcecaoRepository.class);
        MatriculaRepository matriculaRepository = mock(MatriculaRepository.class);
        // Usado so' para resolver o NOME do aluno na listagem de vinculos;
        // os testes de apuracao aqui nao consultam nome.
        var alunoRepository = mock(
                br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository.class);
        service = new JornadaService(jornadaRepository, jornadaDiaRepository, alunoJornadaRepository,
                excecaoRepository, matriculaRepository, alunoRepository);

        when(excecaoRepository.findByTenantIdAndAlunoIdAndDataAndDeletedFalse(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    void jornadaTrocadaNoMeioDoAnoNaoReescreveOPassado() {
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0);
        AccJornada meioPeriodo = jornada("Meio período 5h", 300, RegraExcedente.HORARIO, 15);

        vinculoVigente(MARCO, integral, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 31));
        vinculoVigente(AGOSTO, meioPeriodo, LocalDate.of(2026, 8, 1), null);
        diaDaJornada(integral, 2, true, LocalTime.of(7, 0), LocalTime.of(17, 0), 600);
        diaDaJornada(meioPeriodo, 2, true, LocalTime.of(7, 0), LocalTime.of(12, 0), 300);

        JornadaDoDia emMarco = service.resolverDoDia(TENANT, ALUNO, MARCO);
        JornadaDoDia emAgosto = service.resolverDoDia(TENANT, ALUNO, AGOSTO);

        assertThat(emMarco.cargaMinutos()).isEqualTo(600);
        assertThat(emMarco.regraExcedente()).isEqualTo(RegraExcedente.DURACAO);
        assertThat(emMarco.toleranciaSaidaMin()).isZero();
        assertThat(emAgosto.cargaMinutos()).isEqualTo(300);
        assertThat(emAgosto.regraExcedente()).isEqualTo(RegraExcedente.HORARIO);
        assertThat(emAgosto.toleranciaSaidaMin()).isEqualTo(15);
    }

    @Test
    void excecaoPontualSobrescreveODiaDaJornada() {
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.AMBOS, 10);
        vinculoVigente(MARCO, integral, LocalDate.of(2026, 1, 1), null);
        diaDaJornada(integral, 2, true, LocalTime.of(7, 0), LocalTime.of(17, 0), 600);
        excecao(MARCO, true, LocalTime.of(7, 0), LocalTime.of(13, 0), null, "Consulta médica");

        JornadaDoDia dia = service.resolverDoDia(TENANT, ALUNO, MARCO);

        assertThat(dia.origemExcecao()).isTrue();
        assertThat(dia.saidaPrevista()).isEqualTo(LocalTime.of(13, 0));
        // Carga nao informada na excecao: deriva do proprio horario dela.
        assertThat(dia.cargaMinutos()).isEqualTo(360);
        // Tolerancia e regra continuam vindo do contrato comercial.
        assertThat(dia.toleranciaSaidaMin()).isEqualTo(10);
        assertThat(dia.regraExcedente()).isEqualTo(RegraExcedente.AMBOS);
    }

    @Test
    void excecaoDeAusenciaZeraOPrevistoDoDia() {
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0);
        vinculoVigente(MARCO, integral, LocalDate.of(2026, 1, 1), null);
        diaDaJornada(integral, 2, true, LocalTime.of(7, 0), LocalTime.of(17, 0), 600);
        excecao(MARCO, false, null, null, null, "Viagem em família");

        JornadaDoDia dia = service.resolverDoDia(TENANT, ALUNO, MARCO);

        assertThat(dia.frequenta()).isFalse();
        assertThat(dia.cargaMinutos()).isZero();
    }

    @Test
    void diaDaSemanaSemFrequenciaTemCargaZero() {
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0);
        vinculoVigente(MARCO, integral, LocalDate.of(2026, 1, 1), null);
        diaDaJornada(integral, 2, false, null, null, 0);

        JornadaDoDia dia = service.resolverDoDia(TENANT, ALUNO, MARCO);

        assertThat(dia.frequenta()).isFalse();
        assertThat(dia.cargaMinutos()).isZero();
    }

    @Test
    void alunoSemVinculoVigenteFicaSemContrato() {
        when(alunoJornadaRepository.findVigentesEm(TENANT, ALUNO, MARCO)).thenReturn(List.of());

        JornadaDoDia dia = service.resolverDoDia(TENANT, ALUNO, MARCO);

        assertThat(dia.jornadaId()).isNull();
        assertThat(dia.frequenta()).isFalse();
        assertThat(dia.cargaMinutos()).isZero();
    }

    @Test
    void doisVinculosVigentesSobrepostosSaoRecusadosCom409() {
        TenantContext.setTenantId(TENANT);
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0);
        when(jornadaRepository.findByIdAndTenantIdAndDeletedFalse(integral.getId(), TENANT))
                .thenReturn(Optional.of(integral));

        AccAlunoJornada existente = vinculo(integral, LocalDate.of(2026, 1, 1), null);
        when(alunoJornadaRepository.findSobrepostos(eq(TENANT), eq(ALUNO), any(), any(), isNull()))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.vincular(new AlunoJornadaRequest(
                ALUNO, integral.getId(), LocalDate.of(2026, 3, 1), null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(alunoJornadaRepository, never()).save(any());
    }

    @Test
    void vigenciaComFimAnteriorAoInicioERecusada() {
        TenantContext.setTenantId(TENANT);
        AccJornada integral = jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0);
        when(jornadaRepository.findByIdAndTenantIdAndDeletedFalse(integral.getId(), TENANT))
                .thenReturn(Optional.of(integral));

        assertThatThrownBy(() -> service.vincular(new AlunoJornadaRequest(
                ALUNO, integral.getId(), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loteEncerraOPlanoAnteriorNaVesperaEMantemOPassadoIntacto() {
        TenantContext.setTenantId(TENANT);
        AccJornada novo = jornada("Meio período 5h", 300, RegraExcedente.HORARIO, 0);
        AccAlunoJornada antigo = vinculo(jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0),
                LocalDate.of(2026, 1, 1), null);
        when(alunoJornadaRepository.findByTenantIdAndAlunoIdAndDeletedFalseOrderByVigenciaInicioDesc(TENANT, ALUNO))
                .thenReturn(List.of(antigo));
        when(alunoJornadaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resposta = service.aplicarEmLote(new AplicarJornadaRequest(
                novo.getId(), List.of(ALUNO), LocalDate.of(2026, 8, 1), null, "Turma toda", true));

        assertThat(resposta.aplicados()).isEqualTo(1);
        assertThat(resposta.ignorados()).isEmpty();
        // O plano antigo e' FECHADO, nao apagado: marco continua apurado por ele.
        assertThat(antigo.getVigenciaFim()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(antigo.getDeleted()).isFalse();
    }

    @Test
    void loteNaoEncerraOPlanoAntigoQuandoOAlunoEIgnorado() {
        TenantContext.setTenantId(TENANT);
        AccJornada novo = jornada("Meio período 5h", 300, RegraExcedente.HORARIO, 0);
        // Vinculo futuro, que nao pode ser encerrado na vespera e conflita.
        AccAlunoJornada futuro = vinculo(jornada("Integral 10h", 600, RegraExcedente.DURACAO, 0),
                LocalDate.of(2026, 9, 1), null);
        when(alunoJornadaRepository.findByTenantIdAndAlunoIdAndDeletedFalseOrderByVigenciaInicioDesc(TENANT, ALUNO))
                .thenReturn(List.of(futuro));

        var resposta = service.aplicarEmLote(new AplicarJornadaRequest(
                novo.getId(), List.of(ALUNO), LocalDate.of(2026, 8, 1), null, null, true));

        assertThat(resposta.aplicados()).isZero();
        assertThat(resposta.ignorados()).hasSize(1);
        // Nada foi gravado: o aluno recusado nao pode ficar sem jornada vigente.
        assertThat(futuro.getVigenciaFim()).isNull();
        verify(alunoJornadaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private AccJornada jornada(String nome, int carga, RegraExcedente regra, int toleranciaSaida) {
        AccJornada j = new AccJornada();
        j.setTenantId(TENANT);
        j.setNome(nome);
        j.setRegraExcedente(regra);
        j.setToleranciaSaidaMin(toleranciaSaida);
        atribuirId(j, UUID.randomUUID());
        when(jornadaRepository.findByIdAndTenantIdAndDeletedFalse(j.getId(), TENANT)).thenReturn(Optional.of(j));
        return j;
    }

    private AccAlunoJornada vinculo(AccJornada jornada, LocalDate inicio, LocalDate fim) {
        AccAlunoJornada v = new AccAlunoJornada();
        v.setTenantId(TENANT);
        v.setAlunoId(ALUNO);
        v.setJornadaId(jornada.getId());
        v.setVigenciaInicio(inicio);
        v.setVigenciaFim(fim);
        atribuirId(v, UUID.randomUUID());
        return v;
    }

    private void vinculoVigente(LocalDate data, AccJornada jornada, LocalDate inicio, LocalDate fim) {
        when(alunoJornadaRepository.findVigentesEm(TENANT, ALUNO, data))
                .thenReturn(List.of(vinculo(jornada, inicio, fim)));
    }

    private void diaDaJornada(AccJornada jornada, int diaSemana, boolean frequenta,
                              LocalTime entrada, LocalTime saida, int carga) {
        AccJornadaDia d = new AccJornadaDia();
        d.setTenantId(TENANT);
        d.setJornadaId(jornada.getId());
        d.setDiaSemana(diaSemana);
        d.setFrequenta(frequenta);
        d.setEntradaPrevista(entrada);
        d.setSaidaPrevista(saida);
        d.setCargaMinutos(carga);
        when(jornadaDiaRepository.findByTenantIdAndJornadaIdAndDiaSemanaAndDeletedFalse(
                TENANT, jornada.getId(), diaSemana)).thenReturn(Optional.of(d));
    }

    private void excecao(LocalDate data, boolean frequenta, LocalTime entrada, LocalTime saida,
                         Integer carga, String motivo) {
        AccJornadaExcecao e = new AccJornadaExcecao();
        e.setTenantId(TENANT);
        e.setAlunoId(ALUNO);
        e.setData(data);
        e.setFrequenta(frequenta);
        e.setEntradaPrevista(entrada);
        e.setSaidaPrevista(saida);
        e.setCargaMinutos(carga);
        e.setMotivo(motivo);
        when(excecaoRepository.findByTenantIdAndAlunoIdAndDataAndDeletedFalse(TENANT, ALUNO, data))
                .thenReturn(Optional.of(e));
    }

    private static void atribuirId(BaseEntity entidade, UUID id) {
        try {
            Field campo = BaseEntity.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidade, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
