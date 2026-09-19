package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {
    Page<Dispositivo> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    /**
     * A tela tem campo de busca desde sempre; o controller nao tinha o
     * parametro, entao a pessoa digitava, clicava em Buscar e a lista
     * voltava inteira — sem erro nenhum que indicasse o motivo.
     */
    @Query("select d from Dispositivo d where d.tenantId = :tenantId and d.deleted = false "
         + "and (lower(d.nome) like lower(concat('%', :q, '%')) "
         + "  or lower(coalesce(d.serial, '')) like lower(concat('%', :q, '%')) "
         + "  or lower(coalesce(d.ip, '')) like lower(concat('%', :q, '%')))")
    Page<Dispositivo> buscar(@Param("tenantId") UUID tenantId, @Param("q") String q, Pageable pageable);
    long countByTenantIdAndOnlineTrueAndDeletedFalse(UUID tenantId);
}
