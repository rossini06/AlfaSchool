package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.domain.access.painel.AccPainelDispositivo;

import java.time.Instant;
import java.util.UUID;

/**
 * Nunca carrega o token. Apenas o prefixo, para a coordenacao distinguir
 * as TVs na tela de revogacao.
 */
public record PainelDispositivoResponse(
        UUID id,
        UUID painelId,
        String nome,
        String tokenPrefixo,
        Instant ultimoAcesso,
        String ultimoIp,
        String userAgent,
        boolean revogado,
        Instant revogadoEm
) {
    public static PainelDispositivoResponse from(AccPainelDispositivo d) {
        return new PainelDispositivoResponse(
                d.getId(),
                d.getPainelId(),
                d.getNome(),
                d.getTokenPrefixo(),
                d.getUltimoAcesso(),
                d.getUltimoIp(),
                d.getUserAgent(),
                d.isRevogado(),
                d.getRevogadoEm());
    }
}
