package br.com.alfaschool.backend.application.dashboard.dto;

public record AlertDTO(
        String type,
        String severity,
        String message
) {
}
