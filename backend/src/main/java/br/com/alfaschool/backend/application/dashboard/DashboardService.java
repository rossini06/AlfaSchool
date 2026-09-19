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
import br.com.alfaschool.backend.security.permissao.Permissao;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final SystemHealthService systemHealthService;
    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(DashboardMetricsRepository dashboardMetricsRepository,
                            SystemHealthService systemHealthService,
                            AlunoRepository alunoRepository,
                            TurmaRepository turmaRepository,
                            CursoRepository cursoRepository,
                            MatriculaRepository matriculaRepository,
                            JdbcTemplate jdbcTemplate) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.systemHealthService = systemHealthService;
        this.alunoRepository = alunoRepository;
        this.turmaRepository = turmaRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * O dashboard nao e' um bloco unico: cada cartao mostra dado de uma area
     * diferente, e quem abre a tela nao tem necessariamente acesso a todas.
     *
     * Ja' esteve aberto a qualquer autenticado. O efeito era um responsavel
     * abrir /dashboard e enxergar a matricula nominal dos filhos das outras
     * familias e o indicador de inadimplencia da escola. Nao adianta a tela
     * nao ter o link se o endpoint responde.
     *
     * Por isso a montagem e' por permissao, cartao a cartao: a portaria ve o
     * movimento do dia sem ver inadimplencia, o financeiro ve inadimplencia,
     * e quem so' tem o portal nao chega aqui.
     */
    public DashboardResponseDTO loadDashboard() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }

        UUID unitId = currentUnitId();
        DashboardSnapshot snapshot = dashboardMetricsRepository.buildSnapshot(tenantId, unitId);

        boolean vePessoas = temPermissao(Permissao.ALUNOS_VER);
        boolean veFinanceiro = temPermissao(Permissao.FINANCEIRO_VER);
        boolean veMatriculas = temPermissao(Permissao.MATRICULAS_VER);
        boolean veInfra = temPermissao(Permissao.ESCOLA_GERIR);

        List<StatCardDTO> stats = new ArrayList<>();
        if (vePessoas) {
            stats.add(new StatCardDTO("totalStudents", "Total de Alunos", snapshot.totalStudents()));
            stats.add(new StatCardDTO("totalStaff", "Total de Colaboradores", snapshot.totalStaff()));
            stats.add(new StatCardDTO("attendanceToday", "Presenças Hoje", snapshot.attendanceToday()));
        }
        if (veFinanceiro) {
            stats.add(new StatCardDTO("overduePayments", "Inadimplências", snapshot.overduePayments()));
        }
        stats.add(new StatCardDTO("accessToday", "Acessos Hoje", snapshot.accessToday()));

        // Saude do sistema e' informacao de infraestrutura (versao, banco,
        // fila). Serve para quem administra, nao para quem opera.
        SystemHealthDTO healthDTO = veInfra ? systemHealthService.currentHealth() : null;
        SchoolKpisDTO schoolKpis = vePessoas ? buildSchoolKpis(tenantId) : null;
        // Nome de aluno de terceiro: so' para quem ja' pode abrir a tela de
        // matriculas de qualquer jeito.
        List<Map<String, Object>> recentMatriculas =
                veMatriculas ? buildRecentMatriculas(tenantId) : List.of();

        return new DashboardResponseDTO(stats, buildAlerts(snapshot, veFinanceiro),
                healthDTO, schoolKpis, recentMatriculas);
    }

    /**
     * Le a authority direto do contexto. O formato PERM_<nome> e' o mesmo
     * que o @PreAuthorize usa — nao ha' segunda fonte de verdade.
     */
    private boolean temPermissao(Permissao permissao) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "PERM_" + permissao.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private SchoolKpisDTO buildSchoolKpis(UUID tenantId) {
        long totalAlunos = alunoRepository.countByTenantIdAndDeletedFalse(tenantId);
        long totalTurmas = turmaRepository.countByTenantIdAndDeletedFalse(tenantId);
        long totalCursos = cursoRepository.countByTenantIdAndDeletedFalse(tenantId);
        long matriculasAtivas = matriculaRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "ativa");
        long matriculasCanceladas = matriculaRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "cancelada");
        return new SchoolKpisDTO(totalAlunos, totalTurmas, totalCursos, matriculasAtivas, matriculasCanceladas);
    }

    private List<Map<String, Object>> buildRecentMatriculas(UUID tenantId) {
        try {
            String sql = "SELECT m.id, m.data_matricula, m.status, m.numero_matricula, " +
                         "a.nome AS aluno_nome, t.nome AS turma_nome " +
                         "FROM matriculas m " +
                         "LEFT JOIN alunos a ON a.id = m.aluno_id AND a.deleted = false " +
                         "LEFT JOIN turmas t ON t.id = m.turma_id AND t.deleted = false " +
                         "WHERE m.tenant_id = ? AND m.deleted = false " +
                         "ORDER BY m.created_at DESC LIMIT 8";
            return jdbcTemplate.queryForList(sql, tenantId.toString());
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    private UUID currentUnitId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.unitId();
    }

    private List<AlertDTO> buildAlerts(DashboardSnapshot snapshot, boolean veFinanceiro) {
        List<AlertDTO> alerts = new ArrayList<>();
        if (veFinanceiro && snapshot.overduePayments() > 0) {
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
