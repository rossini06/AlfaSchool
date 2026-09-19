package br.com.alfaschool.backend.application.user.dto;

import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.user.UserAccount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A senha nunca sai daqui, nem cifrada. `perfis` traz id e rotulo porque a
 * tela precisa do id para editar e do rotulo para exibir — devolver so' o
 * nome tecnico obrigaria o frontend a manter uma tabela de traducao propria,
 * que e' como ele acabou com uma lista de perfis inventada
 * ("USER", "GESTOR") que nao existe em escola nenhuma.
 */
public record UserResponse(
        UUID id,
        UUID tenantId,
        String name,
        String email,
        boolean active,
        boolean locked,
        Instant lastLogin,
        List<String> roles,
        List<PerfilResumo> perfis
) {
    public record PerfilResumo(UUID id, String nome, String rotulo) {
        static PerfilResumo de(Role r) {
            return new PerfilResumo(r.getId(), r.getName(),
                    r.getRotulo() != null && !r.getRotulo().isBlank() ? r.getRotulo() : r.getName());
        }
    }

    public static UserResponse from(UserAccount u) {
        return new UserResponse(
                u.getId(), u.getTenantId(), u.getName(), u.getEmail(),
                u.isActive(), u.isLocked(), u.getLastLogin(),
                u.getRoles().stream().map(Role::getName).toList(),
                u.getRoles().stream().map(PerfilResumo::de).toList());
    }
}
