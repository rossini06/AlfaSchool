package br.com.alfaschool.backend.application.shared;

import br.com.alfaschool.backend.domain.shared.AuditLog;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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
