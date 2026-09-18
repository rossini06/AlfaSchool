package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.painel.AccPainelFonte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccPainelFonteRepository extends JpaRepository<AccPainelFonte, UUID> {

    List<AccPainelFonte> findByTenantIdAndPainelId(UUID tenantId, UUID painelId);

    Optional<AccPainelFonte> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Carrega as fontes de varios paineis de uma vez. Usado ao montar a
     * lista de paineis: sem isso seria um SELECT por painel.
     */
    @Query("select f from AccPainelFonte f where f.tenantId = :tenantId and f.painelId in :painelIds")
    List<AccPainelFonte> findByPaineis(@Param("tenantId") UUID tenantId,
                                       @Param("painelIds") Collection<UUID> painelIds);

    /**
     * Quais paineis cobrem este aluno agora. Uma unica query resolve o
     * roteamento do evento ao vivo: sala atual, turma, portaria de chegada
     * ou unidade inteira.
     *
     * Fonte UNIDADE com referencia_id nula significa "a unidade do painel",
     * por isso ela tambem casa quando o unitId bate com o do painel.
     */
    @Query("""
           select distinct f.painelId
           from AccPainelFonte f, AccPainel p
           where f.painelId = p.id
             and f.tenantId = :tenantId
             and p.deleted = false
             and p.ativo = true
             and (
                  (f.escopo = br.com.alfaschool.backend.domain.access.shared.EscopoPainel.SALA
                     and :salaId is not null and f.referenciaId = :salaId)
               or (f.escopo = br.com.alfaschool.backend.domain.access.shared.EscopoPainel.TURMA
                     and :turmaId is not null and f.referenciaId = :turmaId)
               or (f.escopo = br.com.alfaschool.backend.domain.access.shared.EscopoPainel.PORTARIA
                     and :portariaId is not null and f.referenciaId = :portariaId)
               or (f.escopo = br.com.alfaschool.backend.domain.access.shared.EscopoPainel.UNIDADE
                     and (f.referenciaId is null or f.referenciaId = :unitId)
                     and (:unitId is null or p.unitId = :unitId))
             )
           """)
    List<UUID> paineisQueCobrem(@Param("tenantId") UUID tenantId,
                                @Param("unitId") UUID unitId,
                                @Param("turmaId") UUID turmaId,
                                @Param("salaId") UUID salaId,
                                @Param("portariaId") UUID portariaId);
}
