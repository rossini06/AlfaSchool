package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.AutorizacaoRetiradaService;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaResponse;
import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.domain.aluno.Aluno;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.autorizacao;
import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.pessoa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida: o que nasce valendo, o que exige motivo e o que sempre vai
 * para a trilha imutavel.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutorizacaoRetiradaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final UUID PESSOA = UUID.randomUUID();
    private static final UUID AUTORIZACAO = UUID.randomUUID();
    private static final String IP = "200.150.10.1";

    @Mock private AccAutorizacaoRetiradaRepository autorizacaoRepository;
    @Mock private AccAutorizacaoHistoricoRepository historicoRepository;
    @Mock private AccPessoaAutorizadaRepository pessoaRepository;
    @Mock private AlunoRepository alunoRepository;

    private AutorizacaoRetiradaService service;

    @BeforeEach
    void setUp() {
        service = new AutorizacaoRetiradaService(
                autorizacaoRepository, historicoRepository, pessoaRepository, alunoRepository);
        TenantContext.setTenantId(TENANT);

        Aluno aluno = new Aluno();
        ReflectionTestUtils.setField(aluno, "id", ALUNO);
        aluno.setTenantId(TENANT);
        aluno.setNome("Joao");
        when(alunoRepository.findById(ALUNO)).thenReturn(Optional.of(aluno));
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT))
                .thenReturn(Optional.of(pessoa(PESSOA, TENANT, "52998224725", true, true)));
        when(autorizacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("autorizacao de origem PORTAL nasce PENDENTE e nao vale sozinha")
    void portalNascePendente() {
        AutorizacaoRetiradaResponse resposta = service.criar(pedido(true, null, OrigemAutorizacao.PORTAL), IP);

        assertThat(resposta.status()).isEqualTo(StatusAutorizacao.PENDENTE);
        assertThat(resposta.origem()).isEqualTo(OrigemAutorizacao.PORTAL);
        // Sem aprovador: ninguem da escola olhou ainda.
        assertThat(resposta.aprovadoPorUserId()).isNull();
        assertThat(resposta.aprovadoEm()).isNull();
    }

    @Test
    @DisplayName("autorizacao criada pela ESCOLA ja nasce ATIVA")
    void escolaNasceAtiva() {
        AutorizacaoRetiradaResponse resposta = service.criar(pedido(true, null, OrigemAutorizacao.ESCOLA), IP);

        assertThat(resposta.status()).isEqualTo(StatusAutorizacao.ATIVA);
    }

    @Test
    @DisplayName("origem ausente e tratada como ESCOLA")
    void origemAusenteEhEscola() {
        AutorizacaoRetiradaResponse resposta = service.criar(pedido(true, null, null), IP);

        assertThat(resposta.origem()).isEqualTo(OrigemAutorizacao.ESCOLA);
    }

    @Test
    @DisplayName("temporaria sem vigencia_fim e rejeitada na criacao")
    void temporariaSemVigenciaFimRejeitada() {
        assertThatThrownBy(() -> service.criar(pedido(false, null, OrigemAutorizacao.ESCOLA), IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("temporária exige data de fim");

        // Nada foi gravado: a autorizacao eterna nao chega nem a existir.
        verify(autorizacaoRepository, never()).save(any());
        verify(historicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("temporaria com vigencia_fim e aceita")
    void temporariaComVigenciaFimAceita() {
        AutorizacaoRetiradaResponse resposta =
                service.criar(pedido(false, LocalDate.of(2025, 3, 7), OrigemAutorizacao.ESCOLA), IP);

        assertThat(resposta.permanente()).isFalse();
        assertThat(resposta.vigenciaFim()).isEqualTo(LocalDate.of(2025, 3, 7));
    }

    @Test
    @DisplayName("criacao grava historico com acao CRIACAO e status novo")
    void criacaoGravaHistorico() {
        service.criar(pedido(true, null, OrigemAutorizacao.PORTAL), IP);

        AutorizacaoHistorico historico = capturarHistorico();
        assertThat(historico.getAcao()).isEqualTo(AcaoAutorizacao.CRIACAO);
        assertThat(historico.getStatusAnterior()).isNull();
        assertThat(historico.getStatusNovo()).isEqualTo(StatusAutorizacao.PENDENTE);
        assertThat(historico.getIp()).isEqualTo(IP);
        assertThat(historico.getTenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("aprovar leva de PENDENTE para ATIVA e grava historico")
    void aprovarPendente() {
        existente(StatusAutorizacao.PENDENTE, null);

        AutorizacaoRetiradaResponse resposta = service.aprovar(AUTORIZACAO, null, IP);

        assertThat(resposta.status()).isEqualTo(StatusAutorizacao.ATIVA);
        AutorizacaoHistorico historico = capturarHistorico();
        assertThat(historico.getAcao()).isEqualTo(AcaoAutorizacao.APROVACAO);
        assertThat(historico.getStatusAnterior()).isEqualTo(StatusAutorizacao.PENDENTE);
        assertThat(historico.getStatusNovo()).isEqualTo(StatusAutorizacao.ATIVA);
    }

    @Test
    @DisplayName("aprovar nao exige motivo")
    void aprovarNaoExigeMotivo() {
        existente(StatusAutorizacao.PENDENTE, null);

        assertThat(service.aprovar(AUTORIZACAO, null, IP).status()).isEqualTo(StatusAutorizacao.ATIVA);
    }

    @Test
    @DisplayName("aprovar autorizacao ja vencida e recusado")
    void aprovarVencidaRecusado() {
        existente(StatusAutorizacao.PENDENTE, LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.aprovar(AUTORIZACAO, null, IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("fora da vigência");
    }

    @Test
    @DisplayName("aprovar o que nao esta PENDENTE e conflito")
    void aprovarNaoPendenteConflito() {
        existente(StatusAutorizacao.ATIVA, null);

        assertThatThrownBy(() -> service.aprovar(AUTORIZACAO, null, IP))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("suspender EXIGE motivo")
    void suspenderExigeMotivo() {
        existente(StatusAutorizacao.ATIVA, null);

        assertThatThrownBy(() -> service.suspender(AUTORIZACAO, "   ", IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Motivo");
        verify(historicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("suspender com motivo grava historico com o motivo")
    void suspenderGravaMotivo() {
        existente(StatusAutorizacao.ATIVA, null);

        AutorizacaoRetiradaResponse resposta =
                service.suspender(AUTORIZACAO, "Divergencia entre responsaveis", IP);

        assertThat(resposta.status()).isEqualTo(StatusAutorizacao.SUSPENSA);
        AutorizacaoHistorico historico = capturarHistorico();
        assertThat(historico.getAcao()).isEqualTo(AcaoAutorizacao.SUSPENSAO);
        assertThat(historico.getMotivo()).isEqualTo("Divergencia entre responsaveis");
    }

    @Test
    @DisplayName("revogar EXIGE motivo")
    void revogarExigeMotivo() {
        existente(StatusAutorizacao.ATIVA, null);

        assertThatThrownBy(() -> service.revogar(AUTORIZACAO, null, IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Motivo");
        verify(historicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("revogar e terminal: nao se revoga duas vezes")
    void revogarTerminal() {
        existente(StatusAutorizacao.REVOGADA, null);

        assertThatThrownBy(() -> service.revogar(AUTORIZACAO, "Ordem judicial", IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está revogada");
    }

    @Test
    @DisplayName("reativar so parte de SUSPENSA")
    void reativarSoDeSuspensa() {
        existente(StatusAutorizacao.REVOGADA, null);

        assertThatThrownBy(() -> service.reativar(AUTORIZACAO, null, IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("suspensa");
    }

    @Test
    @DisplayName("reativar volta para ATIVA e grava historico")
    void reativarVoltaParaAtiva() {
        existente(StatusAutorizacao.SUSPENSA, null);

        assertThat(service.reativar(AUTORIZACAO, "Acordo revisto", IP).status())
                .isEqualTo(StatusAutorizacao.ATIVA);
        assertThat(capturarHistorico().getAcao()).isEqualTo(AcaoAutorizacao.REATIVACAO);
    }

    @Test
    @DisplayName("editar nao muda status e recusa autorizacao revogada")
    void editarRecusaRevogada() {
        existente(StatusAutorizacao.REVOGADA, null);

        assertThatThrownBy(() -> service.atualizar(AUTORIZACAO, pedido(true, null, OrigemAutorizacao.ESCOLA)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("editar tambem barra temporaria sem vigencia_fim")
    void editarBarraTemporariaSemFim() {
        existente(StatusAutorizacao.ATIVA, null);

        assertThatThrownBy(() -> service.atualizar(AUTORIZACAO, pedido(false, null, OrigemAutorizacao.ESCOLA)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("temporária exige data de fim");
    }

    @Test
    @DisplayName("dias da semana fora de 1..7 sao rejeitados em vez de ignorados")
    void diasSemanaInvalidosRejeitados() {
        AutorizacaoRetiradaRequest invalido = new AutorizacaoRetiradaRequest(
                ALUNO, PESSOA, true, null, null, "0,8", null, null,
                OrigemAutorizacao.ESCOLA, null, null, null);

        assertThatThrownBy(() -> service.criar(invalido, IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("1 (segunda)");
    }

    @Test
    @DisplayName("dias da semana sao normalizados: ordenados e sem repeticao")
    void diasSemanaNormalizados() {
        AutorizacaoRetiradaRequest pedido = new AutorizacaoRetiradaRequest(
                ALUNO, PESSOA, true, null, null, " 5, 1 ,5,3", null, null,
                OrigemAutorizacao.ESCOLA, null, null, null);

        assertThat(service.criar(pedido, IP).diasSemana()).isEqualTo("1,3,5");
    }

    @Test
    @DisplayName("hora fim anterior a hora inicio e rejeitada")
    void horaInvertidaRejeitada() {
        AutorizacaoRetiradaRequest pedido = new AutorizacaoRetiradaRequest(
                ALUNO, PESSOA, true, null, null, null, LocalTime.of(18, 0), LocalTime.of(7, 0),
                OrigemAutorizacao.ESCOLA, null, null, null);

        assertThatThrownBy(() -> service.criar(pedido, IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Hora fim");
    }

    // ------------------------------------------------------------------

    private AutorizacaoRetiradaRequest pedido(boolean permanente, LocalDate vigenciaFim, OrigemAutorizacao origem) {
        return new AutorizacaoRetiradaRequest(
                ALUNO, PESSOA, permanente, null, vigenciaFim, null, null, null,
                origem, null, null, null);
    }

    private void existente(StatusAutorizacao status, LocalDate vigenciaFim) {
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, status);
        a.setVigenciaFim(vigenciaFim);
        when(autorizacaoRepository.findByIdAndTenantIdAndDeletedFalse(AUTORIZACAO, TENANT))
                .thenReturn(Optional.of(a));
    }

    private AutorizacaoHistorico capturarHistorico() {
        ArgumentCaptor<AutorizacaoHistorico> captor = ArgumentCaptor.forClass(AutorizacaoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        return captor.getValue();
    }
}
