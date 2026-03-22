package br.com.alfaschool.backend.application.dashboard;

import br.com.alfaschool.backend.application.dashboard.dto.AlertDTO;
import br.com.alfaschool.backend.application.dashboard.dto.DashboardResponseDTO;
import br.com.alfaschool.backend.application.dashboard.dto.SchoolKpisDTO;
import br.com.alfaschool.backend.application.dashboard.dto.StatCardDTO;
import br.com.alfaschool.backend.application.dashboard.dto.SystemHealthDTO;
import br.com.alfaschool.backend.domain.dashboard.DashboardSnapshot;
import br.com.alfaschool.backend.infrastructure.persistence.dashboard.DashboardMetricsRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.CursoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TurmaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final SystemHealthService systemHealthService;
    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    public DashboardService(DashboardMetricsRepository dashboardMetricsRepository,
                            SystemHealthService systemHealthService,
                            AlunoRepository alunoRepository,
                            TurmaRepository turmaRepository,
                            CursoRepository cursoRepository,
                            MatriculaRepository matriculaRepository) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.systemHealthService = systemHealthService;
        this.alunoRepository = alunoRepository;
        this.turmaRepository = turmaRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    public DashboardResponseDTO loadDashboard() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }

        UUID unitId = currentUnitId();
        DashboardSnapshot snapshot = dashboardMetricsRepository.buildSnapshot(tenantId, unitId);
        SystemHealthDTO healthDTO = systemHealthService.currentHealth();

        List<StatCardDTO> stats = List.of(
                new StatCardDTO("totalStudents", "Total de Alunos", snapshot.totalStudents()),
                new StatCardDTO("totalStaff", "Total de Colaboradores", snapshot.totalStaff()),
                new StatCardDTO("attendanceToday", "Presenças Hoje", snapshot.attendanceToday()),
                new StatCardDTO("overduePayments", "Inadimplências", snapshot.overduePayments()),
                new StatCardDTO("accessToday", "Acessos Hoje", snapshot.accessToday())
        );

        SchoolKpisDTO schoolKpis = buildSchoolKpis(tenantId);

        return new DashboardResponseDTO(stats, buildAlerts(snapshot), healthDTO, schoolKpis);
    }

    private SchoolKpisDTO buildSchoolKpis(UUID tenantId) {
        long totalAlunos = alunoRepository.countByTenantIdAndDeletedFalse(tenantId);
        long totalTurmas = turmaRepository.countByTenantIdAndDeletedFalse(tenantId);
        long totalCursos = cursoRepository.countByTenantIdAndDeletedFalse(tenantId);
        long matriculasAtivas = matriculaRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "ativa");
        long matriculasCanceladas = matriculaRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "cancelada");
        return new SchoolKpisDTO(totalAlunos, totalTurmas, totalCursos, matriculasAtivas, matriculasCanceladas);
    }

    private UUID currentUnitId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.unitId();
    }

    private List<AlertDTO> buildAlerts(DashboardSnapshot snapshot) {
        List<AlertDTO> alerts = new ArrayList<>();
        if (snapshot.overduePayments() > 0) {
            alerts.add(new AlertDTO("FINANCE", "WARNING", "Existem pagamentos em atraso que exigem atenção."));
        }
        if (snapshot.attendanceToday() == 0) {
            alerts.add(new AlertDTO("ACADEMIC", "INFO", "Nenhuma presença registrada hoje."));
        }
        if (alerts.isEmpty()) {
            alerts.add(new AlertDTO("SYSTEM", "SUCCESS", "Nenhum alerta crítico no momento."));
        }
        return alerts;
    }
}
