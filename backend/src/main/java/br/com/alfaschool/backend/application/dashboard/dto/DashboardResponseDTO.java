package br.com.alfaschool.backend.application.dashboard.dto;

import java.util.List;

public record DashboardResponseDTO(
        List<StatCardDTO> stats,
        List<AlertDTO> alerts,
        SystemHealthDTO systemHealth,
        SchoolKpisDTO schoolKpis
) {
}
