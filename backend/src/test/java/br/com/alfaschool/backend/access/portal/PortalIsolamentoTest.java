package br.com.alfaschool.backend.access.portal;

import br.com.alfaschool.backend.application.access.portal.PortalAutorizacaoPort;
import br.com.alfaschool.backend.application.access.portal.PortalIdentidadeService;
import br.com.alfaschool.backend.application.access.portal.PortalPermanenciaPort;
import br.com.alfaschool.backend.application.access.portal.PortalService;
import br.com.alfaschool.backend.application.access.portal.dto.PortalSolicitacaoAutorizacaoRequest;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Isolamento do portal: o responsavel A nao pode enxergar o aluno do
 * responsavel B.
 *
 * <p>E' o teste mais importante desta fatia. O portal expoe a rotina diaria de
 * uma crianca — hora de entrada, hora de saida, quem pode buscar. Um
 * vazamento aqui nao e' um bug de listagem: e' entregar a rotina de uma
 * crianca a um estranho.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalIsolamentoTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID RESPONSAVEL_A = UUID.randomUUID();
    private static final UUID ALUNO_DE_A = UUID.randomUUID();
    private static final UUID ALUNO_DE_B = UUID.randomUUID();

    @Mock
    private PortalIdentidadeService identidadeService;
    @Mock
    private PortalPermanenciaPort permanenciaPort;
    @Mock
    private PortalAutorizacaoPort autorizacaoPort;
    @Mock
    private AccNotificacaoEnvioRepository envioRepository;

    private PortalService portalService;

    @BeforeEach
    void setUp() {
        portalService = new PortalService(identidadeService, permanenciaPort, autorizacaoPort, envioRepository);

        // Identidade do responsavel A: escopo derivado do usuario autenticado.
        when(identidadeService.resolver()).thenReturn(new PortalIdentidadeService.PortalIdentidade(
                TENANT, UUID.randomUUID(), "responsavel.a@exemplo.com",
                Set.of(RESPONSAVEL_A), Set.of(ALUNO_DE_A)));

        // A checagem de escopo e' a logica sob teste: roda de verdade.
        doCallRealMethod().when(identidadeService).exigirAcessoAoAluno(any(), any());
    }

    @Test
    @DisplayName("responsavel A recebe 404 ao pedir o resumo do dia do aluno de B")
    void hojeDeOutroResponsavelNegado() {
        assertThatThrownBy(() -> portalService.hoje(ALUNO_DE_B))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // E, principalmente, o dado nem chega a ser consultado.
        verify(permanenciaPort, never()).resumoDoDia(any(), any(), any());
    }

    @Test
    @DisplayName("responsavel A recebe 404 ao pedir o historico do aluno de B")
    void historicoDeOutroResponsavelNegado() {
        assertThatThrownBy(() -> portalService.historico(ALUNO_DE_B, LocalDate.now().minusDays(7), LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(permanenciaPort, never()).historico(any(), any(), any(), any());
    }

    @Test
    @DisplayName("responsavel A recebe 404 ao pedir as autorizacoes do aluno de B")
    void autorizacoesDeOutroResponsavelNegado() {
        assertThatThrownBy(() -> portalService.autorizacoes(ALUNO_DE_B))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(autorizacaoPort, never()).listar(any(), any());
    }

    @Test
    @DisplayName("responsavel A nao consegue solicitar autorizacao para o aluno de B")
    void solicitacaoParaAlunoDeOutroNegada() {
        PortalSolicitacaoAutorizacaoRequest pedido = new PortalSolicitacaoAutorizacaoRequest(
                "Estranho", "tio", "123", "27999", null, null, null, null, null, "quero buscar");

        assertThatThrownBy(() -> portalService.solicitarAutorizacao(ALUNO_DE_B, pedido))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(autorizacaoPort, never()).solicitar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("responsavel A acessa normalmente o proprio aluno (controle positivo)")
    void acessoAoProprioAlunoPermitido() {
        portalService.autorizacoes(ALUNO_DE_A);
        verify(autorizacaoPort).listar(TENANT, ALUNO_DE_A);
    }

    @Test
    @DisplayName("id nulo tambem e' negado: nunca vira consulta sem filtro")
    void idNuloNegado() {
        assertThatThrownBy(() -> portalService.hoje(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("a solicitacao do portal usa o responsavel do token, nao id vindo do request")
    void solicitanteVemDoToken() {
        PortalSolicitacaoAutorizacaoRequest pedido = new PortalSolicitacaoAutorizacaoRequest(
                "Tia Joana", "tia", "123", "27999", null, null, null, null, null, "buscar as sextas");

        portalService.solicitarAutorizacao(ALUNO_DE_A, pedido);

        verify(autorizacaoPort).solicitar(TENANT, ALUNO_DE_A, RESPONSAVEL_A, pedido);
    }

    @Test
    @DisplayName("usuario sem responsavel vinculado nao enxerga aluno nenhum (falha fechada)")
    void semVinculoNaoVeNada() {
        when(identidadeService.resolver()).thenReturn(new PortalIdentidadeService.PortalIdentidade(
                TENANT, UUID.randomUUID(), "ninguem@exemplo.com", Set.of(), Set.of()));

        assertThat(portalService.meusAlunos()).isEmpty();
        assertThatThrownBy(() -> portalService.hoje(ALUNO_DE_A))
                .isInstanceOf(ResponseStatusException.class);
    }
}
