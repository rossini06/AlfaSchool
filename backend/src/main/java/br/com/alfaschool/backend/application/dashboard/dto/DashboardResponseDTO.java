package br.com.alfaschool.backend.application.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponseDTO(
        List<StatCardDTO> stats,
        List<AlertDTO> alerts,
        SystemHealthDTO systemHealth,
        SchoolKpisDTO schoolKpis,
        List<Map<String, Object>> recentMatriculas
) {
}
