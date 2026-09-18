package br.com.alfaschool.backend.access.portal;

import br.com.alfaschool.backend.application.access.portal.PortalIdentidadeService;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O portal decide QUAIS criancas uma pessoa enxerga. Errar aqui expoe a
 * rotina diaria de um menor — horario de entrada, de saida e quem busca.
 * Por isso estes casos existem.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalVinculoUsuarioTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PortalIdentidadeService service;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        TenantContext.setTenantId(TENANT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(USER, TENANT, null, List.of("MORADOR")), null, List.of()));

        UserAccount conta = new UserAccount();
        conta.setTenantId(TENANT);
        conta.setEmail("casal@exemplo.com");
        ReflectionTestUtils.setField(conta, "id", USER);
        when(userRepository.findByIdAndTenantId(USER, TENANT)).thenReturn(Optional.of(conta));
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Devolve consultas na ordem em que o servico as executa. */
    private void responder(List<?>... resultados) {
        Query[] queries = new Query[resultados.length];
        for (int i = 0; i < resultados.length; i++) {
            queries[i] = mock(Query.class);
            lenient().when(queries[i].setParameter(anyString(), any())).thenReturn(queries[i]);
            lenient().when(queries[i].getResultList()).thenReturn(resultados[i]);
        }
        if (queries.length == 1) {
            when(entityManager.createNativeQuery(anyString())).thenReturn(queries[0]);
        } else {
            when(entityManager.createNativeQuery(anyString()))
                    .thenReturn(queries[0], java.util.Arrays.copyOfRange(queries, 1, queries.length));
        }
    }

    @Test
    @DisplayName("e-mail que casa com dois responsaveis e' recusado, nao somado")
    void emailAmbiguoNaoSomaOsDois() {
        String r1 = UUID.randomUUID().toString();
        String r2 = UUID.randomUUID().toString();
        // 1a consulta: vinculo por user_id -> vazio. 2a: por e-mail -> DOIS.
        responder(List.of(), List.of(r1, r2));

        assertThatThrownBy(() -> service.resolver())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("vinculo por user_id decide sozinho, sem consultar o e-mail")
    void vinculoPorIdTemPrecedencia() {
        String vinculado = UUID.randomUUID().toString();
        String aluno = UUID.randomUUID().toString();
        // 1a: user_id encontra. 2a e 3a: alunos (juncao e legado).
        responder(List.of(vinculado), List.of(aluno), List.of());

        PortalIdentidadeService.PortalIdentidade id = service.resolver();

        assertThat(id.responsavelIds()).containsExactly(UUID.fromString(vinculado));
        assertThat(id.alunoIds()).containsExactly(UUID.fromString(aluno));
    }

    @Test
    @DisplayName("sem vinculo e sem e-mail correspondente, nao ve aluno nenhum")
    void semVinculoFalhaFechada() {
        responder(List.of(), List.of());

        PortalIdentidadeService.PortalIdentidade id = service.resolver();

        assertThat(id.responsavelIds()).isEmpty();
        assertThat(id.alunoIds()).isEmpty();
    }

    @Test
    @DisplayName("aluno de outro responsavel devolve 404, nao 403")
    void alunoDeOutroDevolve404() {
        PortalIdentidadeService.PortalIdentidade id = new PortalIdentidadeService.PortalIdentidade(
                TENANT, USER, "casal@exemplo.com", java.util.Set.of(), java.util.Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.exigirAcessoAoAluno(id, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        // 403 confirmaria que aquele UUID existe na escola.
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
