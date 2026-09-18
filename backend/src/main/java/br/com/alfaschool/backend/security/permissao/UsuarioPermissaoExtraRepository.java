package br.com.alfaschool.backend.security.permissao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsuarioPermissaoExtraRepository extends JpaRepository<UsuarioPermissaoExtra, UUID> {

    List<UsuarioPermissaoExtra> findByTenantIdAndUserIdAndDeletedFalse(UUID tenantId, UUID userId);

    void deleteByTenantIdAndUserId(UUID tenantId, UUID userId);
}
