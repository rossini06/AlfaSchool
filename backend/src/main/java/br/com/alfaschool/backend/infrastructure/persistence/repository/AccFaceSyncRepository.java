package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.biometria.AccFaceSync;
import br.com.alfaschool.backend.domain.access.shared.StatusFaceSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccFaceSyncRepository extends JpaRepository<AccFaceSync, UUID> {

    Optional<AccFaceSync> findByFaceIdAndDispositivoId(UUID faceId, UUID dispositivoId);

    List<AccFaceSync> findByTenantIdAndDispositivoId(UUID tenantId, UUID dispositivoId);

    List<AccFaceSync> findByTenantIdAndFaceId(UUID tenantId, UUID faceId);

    List<AccFaceSync> findByTenantIdAndStatus(UUID tenantId, StatusFaceSync status);

    long countByTenantIdAndDispositivoIdAndStatus(UUID tenantId, UUID dispositivoId, StatusFaceSync status);
}
