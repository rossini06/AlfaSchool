package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.retirada.AccOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.StatusOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccOcorrenciaRepository extends JpaRepository<AccOcorrencia, UUID> {

    Optional<AccOcorrencia> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /** Filtros opcionais num unico @Query: evita explosao de metodos derivados. */
    @Query("""
           select o from AccOcorrencia o
           where o.tenantId = :tenantId
             and o.deleted = false
             and (:unitId is null or o.unitId = :unitId)
             and (:tipo is null or o.tipo = :tipo)
             and (:gravidade is null or o.gravidade = :gravidade)
             and (:status is null or o.status = :status)
           order by o.createdAt desc
           """)
    Page<AccOcorrencia> buscar(@Param("tenantId") UUID tenantId,
                               @Param("unitId") UUID unitId,
                               @Param("tipo") TipoOcorrencia tipo,
                               @Param("gravidade") GravidadeOcorrencia gravidade,
                               @Param("status") StatusOcorrencia status,
                               Pageable pageable);
}
