package br.com.alfaschool.backend.application.user.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID tenantId,
        String name,
        String email,
        boolean active,
        List<String> roles
) {
}
