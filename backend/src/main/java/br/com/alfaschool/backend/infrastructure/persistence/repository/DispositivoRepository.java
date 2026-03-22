package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {
    Page<Dispositivo> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    long countByTenantIdAndOnlineTrueAndDeletedFalse(UUID tenantId);
}
