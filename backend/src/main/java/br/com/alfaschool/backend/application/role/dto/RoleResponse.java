package br.com.alfaschool.backend.application.role.dto;

import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description
) {
}
