package br.com.alfaschool.backend.application.dashboard.dto;

public record SystemHealthDTO(
        String status,
        String db,
        String diskSpace,
        String environment,
        String apiVersion
) {
}
