package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.painel.AccPainelDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccPainelDispositivoRepository extends JpaRepository<AccPainelDispositivo, UUID> {

    Optional<AccPainelDispositivo> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<AccPainelDispositivo> findByTenantIdAndPainelIdAndDeletedFalseOrderByNomeAsc(UUID tenantId, UUID painelId);

    /**
     * Busca pelo hash, nao pelo token. O token em claro so' existe no
     * momento em que foi gerado e na TV; aqui ninguem consegue recupera-lo.
     *
     * A busca NAO filtra revogado: o dispositivo revogado precisa ser
     * encontrado para que a resposta seja 403 com motivo, e nao um 404
     * generico que confundiria o suporte.
     */
    Optional<AccPainelDispositivo> findByTokenHashAndDeletedFalse(String tokenHash);
}
