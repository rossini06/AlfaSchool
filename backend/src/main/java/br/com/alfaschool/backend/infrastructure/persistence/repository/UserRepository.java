package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.user.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
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
}
