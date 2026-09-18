package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda consulta carrega tenantId: um findById solto atravessaria escolas.
 */
@Repository
public interface AccPessoaAutorizadaRepository extends JpaRepository<PessoaAutorizada, UUID> {

    Optional<PessoaAutorizada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<PessoaAutorizada> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<PessoaAutorizada> findByTenantIdAndIdInAndDeletedFalse(UUID tenantId, List<UUID> ids);

    @Query("SELECT p FROM PessoaAutorizada p WHERE p.tenantId = :tenantId AND p.deleted = false "
            + "AND (LOWER(p.nome) LIKE LOWER(CONCAT('%', :q, '%')) OR p.cpf LIKE CONCAT('%', :q, '%'))")
    Page<PessoaAutorizada> search(UUID tenantId, String q, Pageable pageable);

    /** CPF unico por tenant: duas pessoas com o mesmo CPF quebram o match de restricao. */
    boolean existsByTenantIdAndCpfAndDeletedFalse(UUID tenantId, String cpf);

    boolean existsByTenantIdAndCpfAndDeletedFalseAndIdNot(UUID tenantId, String cpf, UUID id);

    List<PessoaAutorizada> findByTenantIdAndResponsavelIdAndDeletedFalse(UUID tenantId, UUID responsavelId);
}
