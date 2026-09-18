package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.estrutura.AccPortaria;
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
public interface AccPortariaRepository extends JpaRepository<AccPortaria, UUID> {

    Page<AccPortaria> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<AccPortaria> findByTenantIdAndUnitIdAndDeletedFalse(UUID tenantId, UUID unitId, Pageable pageable);

    List<AccPortaria> findByTenantIdAndUnitIdAndDeletedFalse(UUID tenantId, UUID unitId);

    Optional<AccPortaria> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    boolean existsByTenantIdAndUnitIdAndNomeIgnoreCaseAndDeletedFalse(UUID tenantId, UUID unitId, String nome);

    /**
     * Conta dispositivos apontando para a portaria. A entidade Dispositivo
     * pertence a outra fatia e ainda nao mapeia portaria_id (coluna criada na
     * V37), entao a checagem vai por query nativa: excluir uma portaria com
     * catraca vinculada quebraria o FK e derrubaria a leitura na porta.
     */
    @Query(value = "SELECT COUNT(*) FROM dispositivos d "
            + "WHERE d.portaria_id = :portariaId AND d.deleted = FALSE", nativeQuery = true)
    long contarDispositivosVinculados(@Param("portariaId") UUID portariaId);
}
