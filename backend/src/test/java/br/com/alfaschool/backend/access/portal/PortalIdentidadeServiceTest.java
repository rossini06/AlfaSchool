package br.com.alfaschool.backend.access.portal;

import br.com.alfaschool.backend.application.access.portal.PortalIdentidadeService;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checagem de escopo isolada do banco: e' logica pura sobre o conjunto de
 * alunos derivado do token.
 */
@ExtendWith(MockitoExtension.class)
class PortalIdentidadeServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO_DE_A = UUID.randomUUID();
    private static final UUID ALUNO_DE_B = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    private PortalIdentidadeService.PortalIdentidade identidadeDeA() {
        return new PortalIdentidadeService.PortalIdentidade(
                TENANT, UUID.randomUUID(), "responsavel.a@exemplo.com",
                Set.of(UUID.randomUUID()), Set.of(ALUNO_DE_A));
    }

    @Test
    @DisplayName("aluno fora do escopo do token devolve 404, nao 403")
    void alunoDeOutroResponsavel404() {
        PortalIdentidadeService service = new PortalIdentidadeService(userRepository);

        assertThatThrownBy(() -> service.exigirAcessoAoAluno(identidadeDeA(), ALUNO_DE_B))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                // 403 confirmaria que o UUID existe na escola; 404 nao entrega nada.
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("aluno dentro do escopo passa")
    void alunoProprioPassa() {
        PortalIdentidadeService service = new PortalIdentidadeService(userRepository);

        assertThatCode(() -> service.exigirAcessoAoAluno(identidadeDeA(), ALUNO_DE_A))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("escopo vazio nega qualquer aluno")
    void escopoVazioNegaTudo() {
        PortalIdentidadeService service = new PortalIdentidadeService(userRepository);
        PortalIdentidadeService.PortalIdentidade semVinculo = new PortalIdentidadeService.PortalIdentidade(
                TENANT, UUID.randomUUID(), "ninguem@exemplo.com", Set.of(), Set.of());

        assertThatThrownBy(() -> service.exigirAcessoAoAluno(semVinculo, ALUNO_DE_A))
                .isInstanceOf(ResponseStatusException.class);
    }
}
