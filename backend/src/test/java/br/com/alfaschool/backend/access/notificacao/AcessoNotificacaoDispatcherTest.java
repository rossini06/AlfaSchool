package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.AcessoNotificacaoDispatcher;
import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcessoNotificacaoDispatcherTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();

    @Mock
    private NotificacaoPort notificacaoPort;
    @Mock
    private AccNotificacaoPreferenciaRepository preferenciaRepository;

    private AcessoNotificacaoDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new AcessoNotificacaoDispatcher(notificacaoPort, preferenciaRepository);
    }

    private AcessoRegistradoEvent evento(UUID eventoId, TitularTipo titularTipo, UUID titularId,
                                         FuncaoDispositivo funcao, ResultadoAcesso resultado,
                                         SentidoAcesso sentido) {
        return new AcessoRegistradoEvent(TENANT, null, eventoId, UUID.randomUUID(), UUID.randomUUID(),
                funcao, titularTipo, titularId, resultado, sentido, null, Instant.now());
    }

    @Test
    @DisplayName("entrada de aluno enfileira ENTRADA_CONFIRMADA com chave ENTRADA:{eventoId}")
    void entrada() {
        UUID eventoId = UUID.randomUUID();

        dispatcher.despachar(evento(eventoId, TitularTipo.ALUNO, ALUNO, FuncaoDispositivo.ALUNO,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA));

        verify(notificacaoPort).enfileirar(eq(TENANT), eq(EventoNotificacao.ENTRADA_CONFIRMADA),
                any(), any(), eq(ALUNO), anyMap(), eq("ENTRADA:" + eventoId));
    }

    @Test
    @DisplayName("saida de aluno enfileira SAIDA_CONFIRMADA com chave SAIDA:{eventoId}")
    void saida() {
        UUID eventoId = UUID.randomUUID();

        dispatcher.despachar(evento(eventoId, TitularTipo.ALUNO, ALUNO, FuncaoDispositivo.ALUNO,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.SAIDA));

        verify(notificacaoPort).enfileirar(eq(TENANT), eq(EventoNotificacao.SAIDA_CONFIRMADA),
                any(), any(), eq(ALUNO), anyMap(), eq("SAIDA:" + eventoId));
    }

    @Test
    @DisplayName("leitura PERMITIDA de responsavel nao vira entrada de aluno")
    void leituraDeResponsavelNaoEntregaAluno() {
        dispatcher.despachar(evento(UUID.randomUUID(), TitularTipo.RESPONSAVEL, UUID.randomUUID(),
                FuncaoDispositivo.RESPONSAVEL, ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA));

        verify(notificacaoPort, never()).enfileirar(any(), any(), any(), any(), any(), anyMap(), anyString());
    }

    @Test
    @DisplayName("NEGADO em leitor de responsavel avisa a familia de cada aluno vinculado")
    void tentativaNaoAutorizadaAvisaCadaFamilia() {
        UUID eventoId = UUID.randomUUID();
        UUID responsavel = UUID.randomUUID();
        UUID outroAluno = UUID.randomUUID();
        when(preferenciaRepository.buscarAlunosDoResponsavel(TENANT.toString(), responsavel.toString()))
                .thenReturn(List.of(ALUNO.toString(), outroAluno.toString()));

        dispatcher.despachar(evento(eventoId, TitularTipo.RESPONSAVEL, responsavel,
                FuncaoDispositivo.RESPONSAVEL, ResultadoAcesso.NEGADO, SentidoAcesso.SAIDA));

        verify(notificacaoPort, times(2)).enfileirar(eq(TENANT),
                eq(EventoNotificacao.TENTATIVA_NAO_AUTORIZADA), any(), any(), any(), anyMap(), anyString());
        // Chave por aluno: sem isso o segundo aviso colidiria com o primeiro e
        // uma das familias ficaria sem saber.
        verify(notificacaoPort).enfileirar(eq(TENANT), eq(EventoNotificacao.TENTATIVA_NAO_AUTORIZADA),
                any(), any(), eq(ALUNO), anyMap(), eq("NAO_AUTORIZADA:" + eventoId + ":" + ALUNO));
    }

    @Test
    @DisplayName("NEGADO sem vinculo nenhum ainda enfileira o alerta, com o titular")
    void tentativaNaoAutorizadaSemVinculo() {
        UUID eventoId = UUID.randomUUID();
        UUID pessoa = UUID.randomUUID();
        when(preferenciaRepository.buscarAlunosDaPessoaAutorizada(anyString(), anyString()))
                .thenReturn(List.of());

        dispatcher.despachar(evento(eventoId, TitularTipo.AUTORIZADA, pessoa,
                FuncaoDispositivo.RESPONSAVEL, ResultadoAcesso.NEGADO, SentidoAcesso.SAIDA));

        verify(notificacaoPort).enfileirar(eq(TENANT), eq(EventoNotificacao.TENTATIVA_NAO_AUTORIZADA),
                eq(TitularTipo.AUTORIZADA), eq(pessoa), any(), anyMap(), eq("NAO_AUTORIZADA:" + eventoId));
    }

    @Test
    @DisplayName("NEGADO em leitor de aluno nao vira alerta de tentativa nao autorizada")
    void negadoEmLeitorDeAlunoNaoAvisa() {
        dispatcher.despachar(evento(UUID.randomUUID(), TitularTipo.ALUNO, ALUNO,
                FuncaoDispositivo.ALUNO, ResultadoAcesso.NEGADO, SentidoAcesso.ENTRADA));

        verify(notificacaoPort, never()).enfileirar(any(), any(), any(), any(), any(), anyMap(), anyString());
    }

    @Test
    @DisplayName("as variaveis do aviso nao carregam foto nem biometria")
    void variaveisSemDadoSensivel() {
        dispatcher.despachar(evento(UUID.randomUUID(), TitularTipo.ALUNO, ALUNO,
                FuncaoDispositivo.ALUNO, ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(notificacaoPort).enfileirar(any(), any(), any(), any(), any(), captor.capture(), anyString());
        assertThat(captor.getValue().keySet()).containsExactlyInAnyOrder("hora", "data");
    }

    @Test
    @DisplayName("listener nunca propaga excecao: falha de aviso nao derruba a portaria")
    void listenerNuncaPropaga() {
        org.mockito.Mockito.doThrow(new IllegalStateException("motor fora do ar"))
                .when(notificacaoPort).enfileirar(any(), any(), any(), any(), any(), anyMap(), anyString());

        assertThatCode(() -> dispatcher.aoRegistrarAcesso(evento(UUID.randomUUID(), TitularTipo.ALUNO, ALUNO,
                FuncaoDispositivo.ALUNO, ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA)))
                .doesNotThrowAnyException();
    }
}
