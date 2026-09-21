package br.com.alfaschool.backend.application.shared;

import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import br.com.alfaschool.backend.domain.shared.AuditLog;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registra uma acao administrativa resolvendo sozinho o tenant (do
     * contexto), o autor (do token) e o IP (da requisicao atual), para nao
     * ter de arrastar HttpServletRequest por toda a cadeia de servicos.
     *
     * <p>Roda na transacao de quem chama: se a acao der rollback, a trilha
     * tambem — nao registra um sucesso que nao aconteceu.
     */
    public void registrarAcao(String action, String entity, UUID entityId) {
        registrarAcao(TenantContext.getTenantId(), action, entity, entityId);
    }

    /**
     * Igual, mas com tenant explicito — para acoes do SaaS, que rodam sem
     * tenant no contexto (TenantContext.semFiltro) e precisam gravar na
     * trilha DA REDE afetada.
     */
    public void registrarAcao(UUID tenantId, String action, String entity, UUID entityId) {
        register(tenantId, autorAtual(), action, entity, entityId, ipDaRequisicaoAtual());
    }

    private static UUID autorAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.userId();
        }
        return null;
    }

    private static String ipDaRequisicaoAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return ContextoAcesso.ipDaRequisicao(attrs.getRequest());
        }
        return null;
    }

    public void register(UUID tenantId, UUID userId, String action, String entity, UUID entityId, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setTimestamp(Instant.now());
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }
}
