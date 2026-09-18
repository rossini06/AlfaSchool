package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccFaceRepository extends JpaRepository<AccFace, UUID> {

    /** Resolucao do titular de um evento: device_user_id -> pessoa. */
    Optional<AccFace> findByTenantIdAndDeviceUserIdAndDeletedFalse(UUID tenantId, Long deviceUserId);

    Optional<AccFace> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccFace> findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(
            UUID tenantId, TitularTipo titularTipo, UUID titularId);

    Page<AccFace> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    /** Delta para o agente local: so' o que mudou desde o ultimo ciclo. */
    List<AccFace> findByTenantIdAndUpdatedAtGreaterThanAndDeletedFalseOrderByUpdatedAtAsc(
            UUID tenantId, Instant updatedAfter, Pageable pageable);

    List<AccFace> findByTenantIdAndAtivoTrueAndDeletedFalse(UUID tenantId);
}
