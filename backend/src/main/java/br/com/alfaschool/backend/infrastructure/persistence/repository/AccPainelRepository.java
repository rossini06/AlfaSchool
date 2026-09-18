package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
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
public interface AccPainelRepository extends JpaRepository<AccPainel, UUID> {

    Optional<AccPainel> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccPainel> findByTenantIdAndSlugAndDeletedFalse(UUID tenantId, String slug);

    boolean existsByTenantIdAndSlugAndDeletedFalse(UUID tenantId, String slug);

    Page<AccPainel> findByTenantIdAndDeletedFalseOrderByNomeAsc(UUID tenantId, Pageable pageable);

    List<AccPainel> findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(UUID tenantId, UUID unitId, TipoPainel tipo);

    /**
     * O slug e' unico por tenant, mas a TV so' manda o slug — nao manda
     * tenant. A resolucao do tenant vem do token do dispositivo, por isso
     * aqui a busca e' por slug em toda a base e o tenant e' conferido
     * depois, contra o dispositivo que apresentou o token.
     */
    @Query("select p from AccPainel p where p.slug = :slug and p.deleted = false and p.ativo = true")
    List<AccPainel> findAtivosPorSlug(@Param("slug") String slug);
}
