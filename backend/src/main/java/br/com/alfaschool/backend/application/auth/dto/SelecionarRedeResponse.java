package br.com.alfaschool.backend.application.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * Sessao do superadministrador dentro de uma rede. So' o access token
 * muda: o refresh continua o do login, preso ao tenant mestre.
 */
public record SelecionarRedeResponse(
        String accessToken,
        UUID tenantId,
        String tenantNome,
        boolean mestre,
        List<String> roles,
        List<String> permissoes,
        List<String> modulos
) {
}
