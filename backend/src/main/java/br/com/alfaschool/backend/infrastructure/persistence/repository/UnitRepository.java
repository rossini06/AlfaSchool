package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.tenant.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    /** Busca por nome ou cidade — a tela de Escolas tem o campo e o controller nao tinha o parametro. */
    @org.springframework.data.jpa.repository.Query(
        "select u from Unit u where u.tenantId = :tenantId and u.deleted = false "
      + "and (lower(u.name) like lower(concat('%', :q, '%')) "
      + "  or lower(coalesce(u.city, '')) like lower(concat('%', :q, '%')))")
    org.springframework.data.domain.Page<Unit> buscar(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
    Page<Unit> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
