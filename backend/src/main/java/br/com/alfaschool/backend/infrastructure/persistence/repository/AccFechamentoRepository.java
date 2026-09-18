package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.permanencia.AccFechamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccFechamentoRepository extends JpaRepository<AccFechamento, UUID> {

    Optional<AccFechamento> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AccFechamento> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * unit_id e' anulavel e a unicidade no banco e' (tenant, unit,
     * competencia). Em JPQL "= null" nunca casa, entao a unidade nula
     * precisa do ramo explicito.
     */
    @Query("""
            select f from AccFechamento f
            where f.tenantId = :tenantId
              and f.competencia = :competencia
              and ((:unitId is null and f.unitId is null) or f.unitId = :unitId)
            """)
    Optional<AccFechamento> buscarPorCompetencia(@Param("tenantId") UUID tenantId,
                                                 @Param("unitId") UUID unitId,
                                                 @Param("competencia") String competencia);

    /** Fechamentos FECHADOS que cobrem a data — a barreira do recalculo. */
    @Query("""
            select f from AccFechamento f
            where f.tenantId = :tenantId
              and f.status = 'FECHADO'
              and f.dataInicio <= :data
              and f.dataFim >= :data
            """)
    List<AccFechamento> buscarFechadosQueCobrem(@Param("tenantId") UUID tenantId, @Param("data") LocalDate data);
}
