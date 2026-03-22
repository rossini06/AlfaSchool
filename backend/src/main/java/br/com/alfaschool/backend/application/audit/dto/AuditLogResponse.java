package br.com.alfaschool.backend.application.audit.dto;

import br.com.alfaschool.backend.domain.shared.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id, UUID tenantId, UUID userId, String action, String entity,
    UUID entityId, Instant timestamp, String ipAddress,
    Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
            log.getId(), log.getTenantId(), log.getUserId(),
            log.getAction(), log.getEntity(), log.getEntityId(),
            log.getTimestamp(), log.getIpAddress(), log.getCreatedAt()
        );
    }
}
