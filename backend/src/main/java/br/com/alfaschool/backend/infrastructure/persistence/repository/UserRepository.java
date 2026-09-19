package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.user.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    @EntityGraph(attributePaths = "roles")
    List<UserAccount> findAllByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCaseAndIdNot(UUID tenantId, String email, UUID id);

    /**
     * Listagem da tela de Usuarios, que ate agora nao existia: a tela
     * chamava GET /usuarios e recebia 404, entao nunca carregou.
     *
     * EntityGraph em roles porque a listagem MOSTRA o perfil de cada um —
     * sem ele seria uma consulta por linha.
     */
    @EntityGraph(attributePaths = "roles")
    @Query("select u from UserAccount u where u.tenantId = :tenantId and u.deleted = false "
         + "and (:q is null or lower(u.name) like lower(concat('%', :q, '%')) "
         + "                or lower(u.email) like lower(concat('%', :q, '%')))")
    Page<UserAccount> buscar(@Param("tenantId") UUID tenantId, @Param("q") String q, Pageable pageable);
}
