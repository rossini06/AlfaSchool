package br.com.alfaschool.backend;

import br.com.alfaschool.backend.application.dashboard.DashboardService;
import br.com.alfaschool.backend.application.dashboard.SystemHealthService;
import br.com.alfaschool.backend.application.dashboard.dto.DashboardResponseDTO;
import br.com.alfaschool.backend.application.dashboard.dto.SystemHealthDTO;
import br.com.alfaschool.backend.domain.dashboard.DashboardSnapshot;
import br.com.alfaschool.backend.infrastructure.persistence.dashboard.DashboardMetricsRepository;
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

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarDashboardComStatsEHealth() {
        UUID tenantId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(UUID.randomUUID(), tenantId, unitId, List.of("USER")),
                        null,
                        List.of()
                )
        );

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

    @Test
    void deveFalharSemTenantContext() {
        assertThatThrownBy(() -> dashboardService.loadDashboard())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }
}
