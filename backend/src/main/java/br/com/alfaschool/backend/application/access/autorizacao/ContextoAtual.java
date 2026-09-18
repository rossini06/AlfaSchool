package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Tenant, usuario e IP da requisicao atual, usados na trilha de historico. */
public final class ContextoAtual {

    private ContextoAtual() {
    }

    /** Sem tenant no contexto nao se opera nada: seria escrever na escola errada. */
    public static UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado");
        }
        return tenantId;
    }

    /** Nulo quando a acao nao veio de um usuario (job de expiracao). */
    public static UUID userIdAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.userId();
    }

    /**
     * IP para a trilha. Atras de proxy o RemoteAddr e' o do proxy, entao o
     * primeiro salto do X-Forwarded-For e' mais util — e' apenas registro,
     * nunca base de decisao de acesso.
     */
    public static String ipDe(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String primeiro = encaminhado.split(",")[0].trim();
            return primeiro.length() > 45 ? primeiro.substring(0, 45) : primeiro;
        }
        String remoto = request.getRemoteAddr();
        return remoto != null && remoto.length() > 45 ? remoto.substring(0, 45) : remoto;
    }
}
