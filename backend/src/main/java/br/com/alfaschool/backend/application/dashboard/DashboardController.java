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

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> dashboard() {
        return ResponseEntity.ok(ApiResponse.of(200, "Dashboard loaded successfully", dashboardService.loadDashboard()));
    }
}
