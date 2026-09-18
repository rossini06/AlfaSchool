package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccCalendarioRepository extends JpaRepository<AccCalendario, UUID> {

    Page<AccCalendario> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<AccCalendario> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<AccCalendario> findByTenantIdAndAnoLetivoAndDeletedFalse(UUID tenantId, Integer anoLetivo);

    /** Calendario ativo da unidade no ano. Mais de um ativo e' erro de cadastro. */
    @Query("SELECT c FROM AccCalendario c WHERE c.tenantId = :tenantId AND c.deleted = false "
            + "AND c.ativo = true AND c.anoLetivo = :ano AND c.unitId = :unitId "
            + "ORDER BY c.createdAt DESC")
    List<AccCalendario> ativosDaUnidade(@Param("tenantId") UUID tenantId,
                                        @Param("unitId") UUID unitId,
                                        @Param("ano") Integer ano);

    /** Calendario ativo global (unit_id nulo), base para todas as unidades. */
    @Query("SELECT c FROM AccCalendario c WHERE c.tenantId = :tenantId AND c.deleted = false "
            + "AND c.ativo = true AND c.anoLetivo = :ano AND c.unitId IS NULL "
            + "ORDER BY c.createdAt DESC")
    List<AccCalendario> ativosGlobais(@Param("tenantId") UUID tenantId, @Param("ano") Integer ano);
}
