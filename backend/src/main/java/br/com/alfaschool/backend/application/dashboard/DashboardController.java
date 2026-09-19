package br.com.alfaschool.backend.application.dashboard;

import br.com.alfaschool.backend.application.dashboard.dto.DashboardResponseDTO;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Quem so' tem o portal da familia nao tem dashboard: a tela e' da
     * operacao da escola. As permissoes abaixo sao as de quem trabalha nela;
     * a montagem do conteudo, cartao a cartao, esta' no service.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_ESCOLA_VER','PERM_ALUNOS_VER','PERM_FINANCEIRO_VER',"
            + "'PERM_ACESSO_PAINEL_VER','PERM_DIARIO_VER','PERM_MATRICULAS_VER')")
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> dashboard() {
        return ResponseEntity.ok(ApiResponse.of(200, "Dashboard loaded successfully", dashboardService.loadDashboard()));
    }
}
