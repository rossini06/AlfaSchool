package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.retirada.AccRetiradaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccRetiradaHistoricoRepository extends JpaRepository<AccRetiradaHistorico, UUID> {

    List<AccRetiradaHistorico> findByTenantIdAndRetiradaIdOrderByCreatedAtAsc(UUID tenantId, UUID retiradaId);
}
