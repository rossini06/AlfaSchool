package br.com.alfaschool.backend.security.jwt;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UUID tenantId,
        UUID unitId,
        List<String> roles
) {
}
