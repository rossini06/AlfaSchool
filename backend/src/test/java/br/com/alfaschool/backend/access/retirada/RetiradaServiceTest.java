package br.com.alfaschool.backend.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.AlunosAutorizadosPort;
import br.com.alfaschool.backend.application.access.retirada.ContextoAlunoPort;
import br.com.alfaschool.backend.application.access.retirada.OcorrenciaRegistroPort;
import br.com.alfaschool.backend.application.access.retirada.RegistrarOcorrenciaEvent;
import br.com.alfaschool.backend.application.access.retirada.RetiradaService;
import br.com.alfaschool.backend.application.access.retirada.RetiradaStatusMudouEvent;
import br.com.alfaschool.backend.application.access.retirada.dto.EntregaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RegistrarSaidaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaManualRequest;
import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.application.access.shared.PermanenciaPort;
import br.com.alfaschool.backend.domain.access.retirada.AccRetirada;
import br.com.alfaschool.backend.domain.access.retirada.AccRetiradaHistorico;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetiradaServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNIDADE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PESSOA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ALUNO_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ALUNO_B = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID TURMA = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID SALA = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Mock private AccRetiradaRepository retiradaRepository;
    @Mock private AccRetiradaHistoricoRepository historicoRepository;
    @Mock private AutorizacaoPort autorizacaoPort;
    @Mock private AlunosAutorizadosPort alunosAutorizadosPort;
    @Mock private ContextoAlunoPort contextoAlunoPort;
    @Mock private PermanenciaPort permanenciaPort;
    @Mock private NotificacaoPort notificacaoPort;
    @Mock private OcorrenciaRegistroPort ocorrenciaPort;
    @Mock private ApplicationEventPublisher publisher;

    @Captor private ArgumentCaptor<AccRetirada> retiradaCaptor;
    @Captor private ArgumentCaptor<RegistrarOcorrenciaEvent> ocorrenciaCaptor;
    @Captor private ArgumentCaptor<AccRetiradaHistorico> historicoCaptor;

    private RetiradaService service;

    /**
     * Os ports de outras fatias chegam por ObjectProvider no codigo real.
     * Aqui um provedor simples embrulha o mock; passar null e' o jeito de
     * simular o modulo que nao esta no ar.
     */
    static <T> ObjectProvider<T> provedorDe(T valor) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return valor;
            }

            @Override
            public T getObject(Object... args) {
                return valor;
            }

            @Override
            public T getIfAvailable() {
                return valor;
            }

            @Override
            public T getIfUnique() {
                return valor;
            }
        };
    }

    @BeforeEach
    void preparar() {
        service = new RetiradaService(retiradaRepository, historicoRepository,
                provedorDe(autorizacaoPort), alunosAutorizadosPort, contextoAlunoPort,
                provedorDe(permanenciaPort), provedorDe(notificacaoPort),
                ocorrenciaPort, publisher);

        TenantContext.setTenantId(TENANT);
        autenticar(USUARIO);

        when(retiradaRepository.save(any(AccRetirada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historicoRepository.save(any(AccRetiradaHistorico.class))).thenAnswer(inv -> inv.getArgument(0));
        when(retiradaRepository.abertasDoAluno(any(), any(), anyCollection())).thenReturn(List.of());
        when(contextoAlunoPort.contextoDe(any(), any(), any()))
                .thenReturn(new ContextoAlunoPort.ContextoAluno(UNIDADE, TURMA, SALA));
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void autenticar(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, TENANT, UNIDADE, List.of("COORDENADOR")), null, List.of()));
    }

    private AcessoRegistradoEvent chegadaDeResponsavel(Instant momento) {
        return new AcessoRegistradoEvent(TENANT, UNIDADE, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), FuncaoDispositivo.RESPONSAVEL, TitularTipo.AUTORIZADA, PESSOA,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.INDEFINIDO, null, momento);
    }

    private AccRetirada retiradaEm(StatusRetirada status) {
        AccRetirada r = new AccRetirada();
        r.setTenantId(TENANT);
        r.setUnitId(UNIDADE);
        r.setAlunoId(ALUNO_A);
        r.setTurmaId(TURMA);
        r.setSalaId(SALA);
        r.setStatus(status);
        r.setSolicitadoEm(Instant.now().minus(Duration.ofMinutes(10)));
        return r;
    }

    private void retiradaExistente(UUID id, AccRetirada retirada) {
        when(retiradaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT))
                .thenReturn(Optional.of(retirada));
    }

    // =================================================================
    // Abertura pela leitura biometrica
    // =================================================================

    @Test
    @DisplayName("Pessoa autorizada para 2 alunos abre 2 retiradas")
    void duasAutorizacoesAbremDuasRetiradas() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(TENANT, PESSOA, momento))
                .thenReturn(List.of(ALUNO_A, ALUNO_B));
        when(autorizacaoPort.verificar(any(), eq(PESSOA), eq(momento)))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));

        List<AccRetirada> abertas = service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        assertEquals(2, abertas.size());
        verify(retiradaRepository, times(2)).save(retiradaCaptor.capture());
        List<AccRetirada> salvas = retiradaCaptor.getAllValues();
        assertEquals(List.of(ALUNO_A, ALUNO_B), salvas.stream().map(AccRetirada::getAlunoId).toList());
        salvas.forEach(r -> assertEquals(StatusRetirada.SOLICITADA, r.getStatus()));
        // Irmaos entram na MESMA posicao: a pessoa chegou uma vez so.
        assertEquals(salvas.get(0).getOrdemChegada(), salvas.get(1).getOrdemChegada());
        verify(ocorrenciaPort, never()).registrar(any());
    }

    @Test
    @DisplayName("Pessoa reconhecida sem autorizacao NAO abre retirada e gera ocorrencia")
    void reconhecidaSemAutorizacaoGeraOcorrencia() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(TENANT, PESSOA, momento)).thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(ALUNO_A, PESSOA, momento))
                .thenReturn(AutorizacaoPort.Veredito.negar(AutorizacaoPort.MotivoNegativa.AUTORIZACAO_NAO_ATIVA, "Autorizacao fora de vigencia"));

        List<AccRetirada> abertas = service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        assertTrue(abertas.isEmpty());
        verify(retiradaRepository, never()).save(any());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA, ocorrenciaCaptor.getValue().tipo());
        assertEquals(PESSOA, ocorrenciaCaptor.getValue().pessoaAutorizadaId());
    }

    @Test
    @DisplayName("Leitura de pessoa desconhecida gera ocorrencia e nenhuma retirada")
    void pessoaDesconhecidaGeraOcorrencia() {
        Instant momento = Instant.now();
        AcessoRegistradoEvent evento = new AcessoRegistradoEvent(TENANT, UNIDADE, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), FuncaoDispositivo.RESPONSAVEL,
                TitularTipo.AUTORIZADA, PESSOA, ResultadoAcesso.NEGADO, SentidoAcesso.INDEFINIDO,
                "face nao reconhecida", momento);

        assertTrue(service.abrirPorReconhecimento(evento).isEmpty());
        verify(retiradaRepository, never()).save(any());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.PESSOA_DESCONHECIDA, ocorrenciaCaptor.getValue().tipo());
    }

    @Test
    @DisplayName("ordem_chegada e sequencial por dia e unidade")
    void ordemChegadaSequencialPorDiaEUnidade() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(TENANT, PESSOA, momento)).thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(any(), any(), any()))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));

        when(retiradaRepository.maiorOrdemChegadaDoDia(eq(TENANT), eq(UNIDADE), any(), any())).thenReturn(0);
        service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        when(retiradaRepository.maiorOrdemChegadaDoDia(eq(TENANT), eq(UNIDADE), any(), any())).thenReturn(1);
        service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        verify(retiradaRepository, times(2)).save(retiradaCaptor.capture());
        assertEquals(1, retiradaCaptor.getAllValues().get(0).getOrdemChegada());
        assertEquals(2, retiradaCaptor.getAllValues().get(1).getOrdemChegada());

        // A janela consultada e do dia e da unidade do evento: duas unidades
        // da mesma rede numeram em paralelo.
        ArgumentCaptor<Instant> inicio = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> fim = ArgumentCaptor.forClass(Instant.class);
        verify(retiradaRepository, times(2))
                .maiorOrdemChegadaDoDia(eq(TENANT), eq(UNIDADE), inicio.capture(), fim.capture());
        assertEquals(Duration.ofDays(1),
                Duration.between(inicio.getValue(), fim.getValue()));
    }

    @Test
    @DisplayName("Aluno com retirada ja aberta nao ganha uma segunda")
    void naoDuplicaRetiradaAberta() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(TENANT, PESSOA, momento)).thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(any(), any(), any()))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));
        when(retiradaRepository.abertasDoAluno(eq(TENANT), eq(ALUNO_A), anyCollection()))
                .thenReturn(List.of(retiradaEm(StatusRetirada.SOLICITADA)));

        assertTrue(service.abrirPorReconhecimento(chegadaDeResponsavel(momento)).isEmpty());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem o modulo de autorizacao no ar, NENHUMA retirada e aberta")
    void semModuloDeAutorizacaoFalhaFechada() {
        RetiradaService semAutorizacao = new RetiradaService(retiradaRepository, historicoRepository,
                provedorDe(null), alunosAutorizadosPort, contextoAlunoPort,
                provedorDe(permanenciaPort), provedorDe(notificacaoPort), ocorrenciaPort, publisher);

        assertTrue(semAutorizacao.abrirPorReconhecimento(chegadaDeResponsavel(Instant.now())).isEmpty());

        // A porta nao abre por omissao: registra-se a falha e para por ali.
        verify(retiradaRepository, never()).save(any());
        verifyNoInteractions(alunosAutorizadosPort);
        verify(ocorrenciaPort).registrar(any());
    }

    @Test
    @DisplayName("Sem o modulo de permanencia, registrar-saida recusa em voz alta")
    void semModuloDePermanenciaRecusaSaida() {
        RetiradaService semPermanencia = new RetiradaService(retiradaRepository, historicoRepository,
                provedorDe(autorizacaoPort), alunosAutorizadosPort, contextoAlunoPort,
                provedorDe(null), provedorDe(notificacaoPort), ocorrenciaPort, publisher);

        UUID id = UUID.randomUUID();
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        entregue.setEntregueEm(Instant.now().minus(Duration.ofMinutes(2)));
        retiradaExistente(id, entregue);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> semPermanencia.registrarSaida(id, null, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        // Nada foi gravado pela metade.
        assertNull(entregue.getSaidaEm());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Evento que nao e chegada de responsavel nao abre nada")
    void eventoIrrelevanteNaoAbreFila() {
        AcessoRegistradoEvent entradaDeAluno = new AcessoRegistradoEvent(TENANT, UNIDADE,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), FuncaoDispositivo.ALUNO,
                TitularTipo.ALUNO, ALUNO_A, ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA, null,
                Instant.now());

        assertTrue(service.abrirPorReconhecimento(entradaDeAluno).isEmpty());
        verifyNoInteractions(alunosAutorizadosPort);
    }

    // =================================================================
    // Transicoes validas
    // =================================================================

    @Test
    void prepararMoveParaPreparando() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.SOLICITADA));

        AccRetirada resultado = service.preparar(id, "10.0.0.1");

        assertEquals(StatusRetirada.PREPARANDO, resultado.getStatus());
        assertNotNull(resultado.getPreparandoEm());
        assertEquals(USUARIO, resultado.getPreparadoPorUserId());
    }

    @Test
    void prontoMoveParaPronto() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PREPARANDO));

        AccRetirada resultado = service.pronto(id, "10.0.0.1");

        assertEquals(StatusRetirada.PRONTO, resultado.getStatus());
        assertNotNull(resultado.getProntoEm());
    }

    @Test
    void entregarMoveParaEntregue() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));

        AccRetirada resultado = service.entregar(id, new EntregaRequest(null), "10.0.0.1");

        assertEquals(StatusRetirada.ENTREGUE, resultado.getStatus());
        assertNotNull(resultado.getEntregueEm());
        assertEquals(USUARIO, resultado.getEntreguePorUserId());
    }

    @Test
    void cancelarExigeMotivo() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.SOLICITADA));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(id, "   ", "10.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());

        AccRetirada resultado = service.cancelar(id, "Responsavel desistiu", "10.0.0.1");
        assertEquals(StatusRetirada.CANCELADA, resultado.getStatus());
        assertEquals("Responsavel desistiu", resultado.getMotivo());
    }

    @Test
    void negarExigeMotivoEGeraOcorrencia() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.SOLICITADA));

        AccRetirada resultado = service.negar(id, "Restricao judicial", "10.0.0.1");

        assertEquals(StatusRetirada.NEGADA, resultado.getStatus());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA, ocorrenciaCaptor.getValue().tipo());
    }

    // =================================================================
    // Transicoes invalidas -> 409
    // =================================================================

    @Test
    @DisplayName("Entregar uma retirada ja entregue devolve 409 com mensagem clara")
    void entregarDuasVezesDevolve409() {
        UUID id = UUID.randomUUID();
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        retiradaExistente(id, entregue);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.entregar(id, null, "10.0.0.1"));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertTrue(erro.getReason().toLowerCase().contains("entregue"), "mensagem: " + erro.getReason());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    void prepararRetiradaCanceladaDevolve409() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.CANCELADA));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.preparar(id, null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void voltarDeProntoParaPreparandoDevolve409() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.preparar(id, null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void cancelarRetiradaNegadaDevolve409() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.NEGADA));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(id, "qualquer", null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    // =================================================================
    // As duas regras que o produto nao pode perder
    // =================================================================

    @Test
    @DisplayName("Entregar sem usuario identificado e rejeitado")
    void entregarSemUsuarioEhRejeitado() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));
        SecurityContextHolder.clearContext();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.entregar(id, new EntregaRequest(null), "10.0.0.1"));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        // Nao gravou nada: nem status, nem historico.
        verify(retiradaRepository, never()).save(any());
        verify(historicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Entregue nunca fica com entregue_por_user_id nulo")
    void entregaSempreCarimbaQuemEntregou() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));

        AccRetirada resultado = service.entregar(id, null, null);

        assertEquals(StatusRetirada.ENTREGUE, resultado.getStatus());
        assertNotNull(resultado.getEntreguePorUserId());
    }

    @Test
    @DisplayName("Entregar NAO encerra a permanencia")
    void entregarNaoChamaPermanencia() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));

        AccRetirada resultado = service.entregar(id, new EntregaRequest(null), "10.0.0.1");

        // A crianca foi entregue ao responsavel mas ainda nao cruzou a
        // catraca de saida: a permanencia continua aberta.
        verify(permanenciaPort, never()).registrarSaida(any(), any(), any(), any());
        verifyNoInteractions(permanenciaPort);
        assertNull(resultado.getSaidaEm());
    }

    @Test
    @DisplayName("Preparar e pronto tambem nao encerram a permanencia")
    void transicoesIntermediariasNaoChamamPermanencia() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.SOLICITADA));
        service.preparar(id, null);

        UUID id2 = UUID.randomUUID();
        retiradaExistente(id2, retiradaEm(StatusRetirada.PREPARANDO));
        service.pronto(id2, null);

        verifyNoInteractions(permanenciaPort);
    }

    @Test
    @DisplayName("Somente registrar-saida encerra a permanencia")
    void registrarSaidaEhOUnicoQueChamaPermanencia() {
        UUID id = UUID.randomUUID();
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        entregue.setEntregueEm(Instant.now().minus(Duration.ofMinutes(2)));
        retiradaExistente(id, entregue);

        Instant momento = Instant.now();
        AccRetirada resultado = service.registrarSaida(id, new RegistrarSaidaRequest(momento, null), "10.0.0.1");

        verify(permanenciaPort).registrarSaida(eq(TENANT), eq(ALUNO_A), eq(momento), eq(null));
        assertEquals(momento, resultado.getSaidaEm());
        // O status nao muda: ENTREGUE continua sendo o fim da fila.
        assertEquals(StatusRetirada.ENTREGUE, resultado.getStatus());
    }

    @Test
    void registrarSaidaDuasVezesDevolve409() {
        UUID id = UUID.randomUUID();
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        entregue.setEntregueEm(Instant.now().minus(Duration.ofMinutes(5)));
        entregue.setSaidaEm(Instant.now().minus(Duration.ofMinutes(1)));
        retiradaExistente(id, entregue);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrarSaida(id, null, null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(permanenciaPort, never()).registrarSaida(any(), any(), any(), any());
    }

    @Test
    void registrarSaidaAntesDaEntregaEhRejeitado() {
        UUID id = UUID.randomUUID();
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        entregue.setEntregueEm(Instant.now());
        retiradaExistente(id, entregue);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrarSaida(id,
                        new RegistrarSaidaRequest(Instant.now().minus(Duration.ofHours(1)), null), null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(permanenciaPort, never()).registrarSaida(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Saida pela catraca carimba saida_em sem chamar registrarSaida")
    void saidaPorEventoNaoChamaPermanencia() {
        AccRetirada entregue = retiradaEm(StatusRetirada.ENTREGUE);
        entregue.setEntregueEm(Instant.now().minus(Duration.ofMinutes(3)));
        when(retiradaRepository.findByTenantIdAndAlunoIdAndDeletedFalseOrderBySolicitadoEmDesc(TENANT, ALUNO_A))
                .thenReturn(List.of(entregue));

        Instant saida = Instant.now();
        UUID eventoId = UUID.randomUUID();
        service.marcarSaidaPorEvento(new AcessoRegistradoEvent(TENANT, UNIDADE, eventoId,
                UUID.randomUUID(), UUID.randomUUID(), FuncaoDispositivo.ALUNO, TitularTipo.ALUNO,
                ALUNO_A, ResultadoAcesso.PERMITIDO, SentidoAcesso.SAIDA, null, saida));

        assertEquals(saida, entregue.getSaidaEm());
        assertEquals(eventoId, entregue.getSaidaEventoId());
        // A fatia de permanencia escuta o mesmo evento: chamar aqui
        // fecharia a permanencia duas vezes.
        verifyNoInteractions(permanenciaPort);
    }

    // =================================================================
    // Tempo de espera
    // =================================================================

    @Test
    @DisplayName("Tempo de espera e calculado entre solicitado_em e entregue_em")
    void tempoDeEsperaEntreChegadaEEntrega() {
        AccRetirada retirada = new AccRetirada();
        // O pai chegou 17h00 e recebeu o filho 17h20.
        Instant chegada = Instant.parse("2026-09-18T20:00:00Z");
        retirada.setSolicitadoEm(chegada);
        retirada.setEntregueEm(chegada.plus(Duration.ofMinutes(20)));

        assertEquals(20L, retirada.tempoEsperaMinutos(chegada.plus(Duration.ofHours(3))));
    }

    @Test
    @DisplayName("Enquanto nao entrega, o relogio da espera continua correndo")
    void tempoDeEsperaEmAberto() {
        AccRetirada retirada = new AccRetirada();
        Instant chegada = Instant.parse("2026-09-18T20:00:00Z");
        retirada.setSolicitadoEm(chegada);

        assertEquals(35L, retirada.tempoEsperaMinutos(chegada.plus(Duration.ofMinutes(35))));
    }

    @Test
    void tempoDeEsperaSemChegadaEhZero() {
        assertEquals(0L, new AccRetirada().tempoEsperaMinutos(Instant.now()));
    }

    // =================================================================
    // Retirada manual
    // =================================================================

    @Test
    @DisplayName("Retirada manual exige motivo e gera ocorrencia de auditoria")
    void retiradaManualGeraOcorrencia() {
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, null,
                "Vizinha Maria", "12345678900", "Mae internada, autorizacao por telefone", null);

        AccRetirada resultado = service.abrirManual(request, "10.0.0.9");

        assertEquals(StatusRetirada.SOLICITADA, resultado.getStatus());
        assertTrue(resultado.isRetiradaManual());
        assertTrue(resultado.getObservacao().contains("Vizinha Maria"));
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.RETIRADA_MANUAL, ocorrenciaCaptor.getValue().tipo());
    }

    @Test
    void retiradaManualSemMotivoEhRejeitada() {
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, null,
                "Vizinha Maria", null, "  ", null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.abrirManual(request, null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    void retiradaManualSemIdentificarQuemRetiraEhRejeitada() {
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, null,
                null, null, "Motivo qualquer", null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.abrirManual(request, null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void retiradaManualSemUsuarioEhRejeitada() {
        SecurityContextHolder.clearContext();
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, null,
                "Vizinha Maria", null, "Motivo", null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.abrirManual(request, null));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    // =================================================================
    // Restricao judicial: precedencia sobre qualquer caminho
    //
    // Estes testes existem por causa de um furo real que o sistema teve: o
    // /verificar dizia "restricao judicial vigente", e a retirada manual —
    // que nunca consultava o AutorizacaoPort — abria, preparava e ENTREGAVA
    // a crianca mesmo assim. A regra 3 do CLAUDE.md nao pode depender de
    // qual porta a pessoa usou.
    // =================================================================

    private AutorizacaoPort.Veredito restricaoJudicial() {
        return AutorizacaoPort.Veredito.negar(AutorizacaoPort.MotivoNegativa.RESTRICAO_JUDICIAL,
                "Restricao judicial vigente");
    }

    @Test
    @DisplayName("Retirada manual de pessoa com restricao judicial e' bloqueada e nada e' salvo")
    void retiradaManualComRestricaoJudicialEhBloqueada() {
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any())).thenReturn(restricaoJudicial());
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, PESSOA,
                "Carlos Silva", "12345678900", "Pai veio buscar, leitor quebrado", null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.abrirManual(request, "10.0.0.9"));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verify(retiradaRepository, never()).save(any());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.RESTRICAO_JUDICIAL, ocorrenciaCaptor.getValue().tipo());
        assertEquals(GravidadeOcorrencia.CRITICA, ocorrenciaCaptor.getValue().gravidade());
    }

    @Test
    @DisplayName("Retirada manual de quem apenas nao tem autorizacao passa, mas com ocorrencia ALTA e o motivo real")
    void retiradaManualSemAutorizacaoPassaComOcorrenciaAlta() {
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any())).thenReturn(
                AutorizacaoPort.Veredito.negar(AutorizacaoPort.MotivoNegativa.AUTORIZACAO_NAO_ATIVA,
                        "Autorizacao vencida em 01/09"));
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, PESSOA,
                "Tia Ana", null, "Mae confirmou por telefone", null);

        AccRetirada resultado = service.abrirManual(request, "10.0.0.9");

        assertEquals(StatusRetirada.SOLICITADA, resultado.getStatus());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        RegistrarOcorrenciaEvent ocorrencia = ocorrenciaCaptor.getValue();
        assertEquals(TipoOcorrencia.RETIRADA_MANUAL, ocorrencia.tipo());
        assertEquals(GravidadeOcorrencia.ALTA, ocorrencia.gravidade());
        // Sem isto a coordenacao revisa o alarme sem saber qual era o impedimento.
        assertTrue(ocorrencia.descricao().contains("Autorizacao vencida em 01/09"));
    }

    @Test
    @DisplayName("Retirada manual de pessoa autorizada segue rotina: ocorrencia MEDIA")
    void retiradaManualDePessoaAutorizadaEhRotina() {
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any()))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, PESSOA,
                "Mae", null, "Leitor da portaria fora do ar", null);

        service.abrirManual(request, "10.0.0.9");

        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(GravidadeOcorrencia.MEDIA, ocorrenciaCaptor.getValue().gravidade());
    }

    @Test
    @DisplayName("Sem o modulo de autorizacao no ar, a retirada manual falha fechada")
    void retiradaManualFalhaFechadaSemModuloDeAutorizacao() {
        service = new RetiradaService(retiradaRepository, historicoRepository,
                provedorDe((AutorizacaoPort) null), alunosAutorizadosPort, contextoAlunoPort,
                provedorDe(permanenciaPort), provedorDe(notificacaoPort), ocorrenciaPort, publisher);
        RetiradaManualRequest request = new RetiradaManualRequest(ALUNO_A, UNIDADE, null, PESSOA,
                "Mae", null, "Leitor fora do ar", null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.abrirManual(request, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Restricao cadastrada DEPOIS da chegada bloqueia a entrega")
    void entregaEhBloqueadaPorRestricaoRegistradaAposAChegada() {
        UUID id = UUID.randomUUID();
        AccRetirada retirada = retiradaEm(StatusRetirada.PRONTO);
        retirada.setPessoaAutorizadaId(PESSOA);
        retiradaExistente(id, retirada);
        // Na abertura estava liberado; a medida protetiva chegou no meio da tarde.
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any())).thenReturn(restricaoJudicial());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.entregar(id, new EntregaRequest(null), "10.0.0.9"));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        // A retirada continua aberta: quem decide o que fazer e' a coordenacao.
        assertEquals(StatusRetirada.PRONTO, retirada.getStatus());
        assertNull(retirada.getEntregueEm());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.RESTRICAO_JUDICIAL, ocorrenciaCaptor.getValue().tipo());
    }

    @Test
    @DisplayName("Entrega normal nao e' afetada pela reconferencia")
    void entregaSegueNormalQuandoNaoHaRestricao() {
        UUID id = UUID.randomUUID();
        AccRetirada retirada = retiradaEm(StatusRetirada.PRONTO);
        retirada.setPessoaAutorizadaId(PESSOA);
        retiradaExistente(id, retirada);
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any()))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));

        AccRetirada resultado = service.entregar(id, new EntregaRequest(null), "10.0.0.9");

        assertEquals(StatusRetirada.ENTREGUE, resultado.getStatus());
        assertNotNull(resultado.getEntregueEm());
    }

    @Test
    @DisplayName("Pessoa com restricao judicial identificada na portaria gera ocorrencia CRITICA")
    void reconhecimentoComRestricaoJudicialGeraOcorrencia() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(eq(TENANT), eq(PESSOA), any()))
                .thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any())).thenReturn(restricaoJudicial());

        List<AccRetirada> abertas = service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        assertTrue(abertas.isEmpty());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.RESTRICAO_JUDICIAL, ocorrenciaCaptor.getValue().tipo());
        assertEquals(GravidadeOcorrencia.CRITICA, ocorrenciaCaptor.getValue().gravidade());
    }

    @Test
    @DisplayName("Autorizacao vencida na portaria gera a ocorrencia generica, nao a de restricao judicial")
    void reconhecimentoSemAutorizacaoGeraOcorrenciaGenerica() {
        when(alunosAutorizadosPort.alunosCandidatos(eq(TENANT), eq(PESSOA), any()))
                .thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(eq(ALUNO_A), eq(PESSOA), any())).thenReturn(
                AutorizacaoPort.Veredito.negar(AutorizacaoPort.MotivoNegativa.AUTORIZACAO_NAO_ATIVA,
                        "Autorizacao vencida"));

        assertTrue(service.abrirPorReconhecimento(chegadaDeResponsavel(Instant.now())).isEmpty());
        verify(ocorrenciaPort).registrar(ocorrenciaCaptor.capture());
        assertEquals(TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA, ocorrenciaCaptor.getValue().tipo());
    }

    // =================================================================
    // Trilha e publicacao
    // =================================================================

    @Test
    @DisplayName("Toda transicao grava historico append-only com quem, de onde e de/para")
    void transicaoGravaHistorico() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.SOLICITADA));

        service.preparar(id, "10.0.0.7");

        verify(historicoRepository).save(historicoCaptor.capture());
        AccRetiradaHistorico h = historicoCaptor.getValue();
        assertEquals(StatusRetirada.SOLICITADA, h.getStatusAnterior());
        assertEquals(StatusRetirada.PREPARANDO, h.getStatusNovo());
        assertEquals(USUARIO, h.getUserId());
        assertEquals("10.0.0.7", h.getIp());
        assertEquals(TENANT, h.getTenantId());
    }

    @Test
    void transicaoPublicaEventoParaOsPaineis() {
        UUID id = UUID.randomUUID();
        retiradaExistente(id, retiradaEm(StatusRetirada.PRONTO));

        service.entregar(id, null, null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        RetiradaStatusMudouEvent evento = (RetiradaStatusMudouEvent) captor.getValue();
        assertEquals(StatusRetirada.ENTREGUE, evento.statusNovo());
        assertEquals(StatusRetirada.PRONTO, evento.statusAnterior());
        assertEquals(SALA, evento.salaId());
        assertEquals("retirada.entregue", evento.nomeSse());
    }

    @Test
    void abrirNotificaAFamilia() {
        Instant momento = Instant.now();
        when(alunosAutorizadosPort.alunosCandidatos(TENANT, PESSOA, momento)).thenReturn(List.of(ALUNO_A));
        when(autorizacaoPort.verificar(any(), any(), any()))
                .thenReturn(AutorizacaoPort.Veredito.permitir(UUID.randomUUID()));

        service.abrirPorReconhecimento(chegadaDeResponsavel(momento));

        verify(notificacaoPort).enfileirar(eq(TENANT), any(), any(), any(), eq(ALUNO_A), anyMap(), anyString());
    }

    @Test
    @DisplayName("Sem tenant no contexto, nenhuma transicao acontece")
    void semTenantNaoOpera() {
        TenantContext.clear();
        UUID id = UUID.randomUUID();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.preparar(id, null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    @DisplayName("Retirada de outro tenant nao e encontrada")
    void retiradaDeOutroTenantNaoEhVisivel() {
        UUID id = UUID.randomUUID();
        when(retiradaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.entregar(id, null, null));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}
