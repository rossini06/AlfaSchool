package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.equipamento.AccDispositivoZona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccDispositivoZonaRepository extends JpaRepository<AccDispositivoZona, UUID> {

    List<AccDispositivoZona> findByTenantIdAndDispositivoId(UUID tenantId, UUID dispositivoId);

    List<AccDispositivoZona> findByTenantIdAndZonaId(UUID tenantId, UUID zonaId);

    Optional<AccDispositivoZona> findByDispositivoIdAndZonaId(UUID dispositivoId, UUID zonaId);

    void deleteByDispositivoIdAndZonaId(UUID dispositivoId, UUID zonaId);
}
