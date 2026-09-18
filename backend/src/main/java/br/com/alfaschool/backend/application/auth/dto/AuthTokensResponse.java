package br.com.alfaschool.backend.application.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        UUID tenantId,
        List<String> roles,
        /** Permissoes efetivas: e' o que a tela usa para montar o menu. */
        List<String> permissoes,
        boolean mustChangePassword
) {
}
