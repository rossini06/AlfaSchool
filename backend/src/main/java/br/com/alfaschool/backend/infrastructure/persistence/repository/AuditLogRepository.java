package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.shared.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(UUID tenantId, String action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntity(UUID tenantId, String entity, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp >= :from AND a.timestamp <= :to")
    Page<AuditLog> findByTenantIdAndTimestampBetween(UUID tenantId, Instant from, Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entity IS NULL OR a.entity = :entity) " +
           "AND (:from IS NULL OR a.timestamp >= :from) " +
           "AND (:to IS NULL OR a.timestamp <= :to)")
    Page<AuditLog> findFiltered(UUID tenantId, String action, String entity, Instant from, Instant to, Pageable pageable);
}
