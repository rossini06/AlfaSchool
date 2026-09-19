package br.com.alfaschool.backend;

import br.com.alfaschool.backend.application.dashboard.DashboardService;
import br.com.alfaschool.backend.application.dashboard.SystemHealthService;
import br.com.alfaschool.backend.application.dashboard.dto.DashboardResponseDTO;
import br.com.alfaschool.backend.application.dashboard.dto.SystemHealthDTO;
import br.com.alfaschool.backend.domain.dashboard.DashboardSnapshot;
import br.com.alfaschool.backend.infrastructure.persistence.dashboard.DashboardMetricsRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.CursoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TurmaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import br.com.alfaschool.backend.security.permissao.Permissao;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardMetricsRepository dashboardMetricsRepository;

    @Mock
    private SystemHealthService systemHealthService;

    // O DashboardService ganhou estas dependencias depois que o teste foi
    // escrito, e sem os mocks o @InjectMocks deixava os campos nulos: o
    // teste quebrava com NPE em buildSchoolKpis, nao por bug de negocio.
    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Autentica com as permissoes informadas, no formato PERM_<nome>. */
    private void autenticar(UUID tenantId, UUID unitId, Permissao... permissoes) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(permissoes)
                .map(p -> new SimpleGrantedAuthority("PERM_" + p.name()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(UUID.randomUUID(), tenantId, unitId, List.of("USER")),
                        null,
                        authorities
                )
        );
    }

    @Test
    void deveRetornarDashboardComStatsEHealth() {
        UUID tenantId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        autenticar(tenantId, unitId, Permissao.ALUNOS_VER, Permissao.FINANCEIRO_VER,
                Permissao.MATRICULAS_VER, Permissao.ESCOLA_GERIR);

        when(dashboardMetricsRepository.buildSnapshot(eq(tenantId), eq(unitId)))
                .thenReturn(new DashboardSnapshot(120, 18, 110, 4, 33));
        when(systemHealthService.currentHealth())
                .thenReturn(new SystemHealthDTO("UP", "UP", "UP", "DEV", "0.0.1"));

        DashboardResponseDTO response = dashboardService.loadDashboard();

        assertThat(response.stats()).hasSize(5);
        assertThat(response.stats().getFirst().key()).isEqualTo("totalStudents");
        assertThat(response.alerts()).isNotEmpty();
        assertThat(response.systemHealth().status()).isEqualTo("UP");
    }

    /**
     * O dashboard e' montado cartao a cartao pela permissao de quem pede.
     * A portaria acompanha o movimento do dia; o numero de inadimplentes
     * e o nome de aluno de terceiro nao sao assunto dela.
     */
    @Test
    void portariaVeOMovimentoMasNaoAInadimplencia() {
        UUID tenantId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        autenticar(tenantId, unitId, Permissao.ACESSO_PAINEL_VER);

        when(dashboardMetricsRepository.buildSnapshot(eq(tenantId), eq(unitId)))
                .thenReturn(new DashboardSnapshot(120, 18, 110, 4, 33));

        DashboardResponseDTO response = dashboardService.loadDashboard();

        assertThat(response.stats()).extracting("key").containsExactly("accessToday");
        assertThat(response.stats()).extracting("key").doesNotContain("overduePayments");
        assertThat(response.recentMatriculas()).isEmpty();
        assertThat(response.schoolKpis()).isNull();
        assertThat(response.systemHealth()).isNull();
        // O alerta de pagamento em atraso tambem e' financeiro.
        assertThat(response.alerts()).extracting("type").doesNotContain("FINANCE");
    }

    @Test
    void deveFalharSemTenantContext() {
        assertThatThrownBy(() -> dashboardService.loadDashboard())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }
}
