package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Quem esta' pedindo e de onde. Centralizado porque a fatia inteira depende
 * de duas perguntas: qual tenant e qual usuario.
 *
 * usuarioObrigatorio() existe para o ato de entrega: a regra do produto e'
 * que ninguem entrega crianca anonimamente. Se o contexto nao souber dizer
 * quem e', a operacao para aqui, antes de tocar o banco.
 */
public final class ContextoAcesso {

    private ContextoAcesso() {
    }

    public static UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }

    public static AuthenticatedUser usuarioOuNulo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal;
    }

    public static UUID userIdOuNulo() {
        AuthenticatedUser usuario = usuarioOuNulo();
        return usuario == null ? null : usuario.userId();
    }

    /**
     * Usado onde a identificacao e' o proprio ato: entrega, cancelamento,
     * negativa, tratativa de ocorrencia.
     */
    public static UUID userIdObrigatorio() {
        UUID userId = userIdOuNulo();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Usuario nao identificado: esta operacao exige colaborador autenticado");
        }
        return userId;
    }

    /**
     * IP de origem para a trilha. Respeita X-Forwarded-For porque o backend
     * fica atras de proxy e, sem isso, todo registro ficaria com o IP do
     * balanceador — auditoria inutil.
     */
    public static String ipDaRequisicao(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            int virgula = encaminhado.indexOf(',');
            String primeiro = virgula > 0 ? encaminhado.substring(0, virgula) : encaminhado;
            return primeiro.trim();
        }
        return request.getRemoteAddr();
    }
}
