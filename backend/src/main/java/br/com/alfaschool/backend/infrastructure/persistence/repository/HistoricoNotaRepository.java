package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.diario.HistoricoNota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoNotaRepository extends JpaRepository<HistoricoNota, UUID> {

    List<HistoricoNota> findByTenantIdAndNotaIdOrderByAlteradoEmDesc(UUID tenantId, UUID notaId);

    Page<HistoricoNota> findByTenantIdAndNotaIdOrderByAlteradoEmDesc(UUID tenantId, UUID notaId, Pageable pageable);
}
