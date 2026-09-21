package br.com.alfaschool.backend.application.user;

import br.com.alfaschool.backend.application.user.dto.AtualizarUsuarioRequest;
import br.com.alfaschool.backend.application.user.dto.CreateUserRequest;
import br.com.alfaschool.backend.application.user.dto.UserResponse;
import br.com.alfaschool.backend.domain.role.Permission;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.security.permissao.GuardaConcessao;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        UUID tenantId = requiredTenant();
        if (userRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado para este tenant");
        }

        UserAccount user = new UserAccount();
        user.setTenantId(tenantId);
        user.setName(request.nome().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.senha()));
        user.setActive(request.ativo() == null || request.ativo());
        user.setLocked(false);
        user.setFailedAttempts(0);
        // Quem define a senha aqui e' outra pessoa. A troca no primeiro
        // acesso e' o que impede o administrador de seguir sabendo a senha
        // de quem cadastrou.
        user.setMustChangePassword(true);
        if (request.perfis() != null && !request.perfis().isEmpty()) {
            aplicarPerfis(user, tenantId, request.perfis());
        }

        UserAccount saved = userRepository.save(user);
        return map(saved);
    }

    @Transactional
    public UserResponse assignRole(UUID userId, UUID roleId) {
        UUID tenantId = requiredTenant();
        UserAccount user = userRepository.findById(userId)
                .filter(it -> tenantId.equals(it.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Role role = roleRepository.findById(roleId)
                .filter(it -> tenantId.equals(it.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrada"));

        // Ninguem se promove alem do que tem, nem atribui SUPER_ADMIN pela tela.
        GuardaConcessao.exigirPapelConcedivel(role.getName());
        GuardaConcessao.exigirNaoAmpliar(role.getPermissions().stream().map(Permission::getName).toList());

        user.getRoles().add(role);
        return map(userRepository.save(user));
    }

    /**
     * Listagem que a tela de Usuarios sempre esperou e que nao existia — ela
     * chamava GET /usuarios e recebia 404, entao nunca carregou uma linha.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listar(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        String termo = (q == null || q.isBlank()) ? null : q.trim();
        return userRepository.buscar(tenantId, termo, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse buscar(UUID id) {
        return UserResponse.from(carregar(id));
    }

    /**
     * Edicao. Nao permite trocar o proprio perfil: quem tem PERFIS_GERIR
     * poderia tirar a propria permissao por engano e trancar a escola inteira
     * fora da administracao — sem ninguem para devolver o acesso.
     */
    @Transactional
    public UserResponse atualizar(UUID id, AtualizarUsuarioRequest request) {
        UUID tenantId = requiredTenant();
        UserAccount user = carregar(id);

        if (userRepository.existsByTenantIdAndEmailIgnoreCaseAndIdNot(tenantId, request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe outro usuário com este e-mail nesta escola.");
        }

        user.setName(request.nome().trim());
        user.setEmail(request.email().trim().toLowerCase());
        if (request.senha() != null && !request.senha().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.senha()));
            // Senha definida por outra pessoa: a troca no proximo login e'
            // o que impede o administrador de continuar sabendo a senha.
            user.setMustChangePassword(true);
            user.setLocked(false);
            user.setFailedAttempts(0);
        }
        if (request.ativo() != null) {
            if (!request.ativo() && ehOProprioUsuario(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Você não pode desativar o próprio acesso.");
            }
            user.setActive(request.ativo());
        }
        if (request.perfis() != null) {
            if (ehOProprioUsuario(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Você não pode alterar os próprios perfis. Peça a outro administrador.");
            }
            aplicarPerfis(user, tenantId, request.perfis());
        }
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Exclusao logica. O usuario some das telas mas a trilha de auditoria
     * continua apontando para ele — apagar a linha deixaria "alterado por"
     * orfao em todo registro que ele tocou.
     */
    @Transactional
    public void excluir(UUID id) {
        UserAccount user = carregar(id);
        if (ehOProprioUsuario(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode excluir o próprio acesso.");
        }
        user.setDeleted(true);
        user.setActive(false);
        userRepository.save(user);
    }

    private void aplicarPerfis(UserAccount user, UUID tenantId, List<UUID> perfisIds) {
        user.getRoles().clear();
        java.util.Set<String> concedidas = new java.util.HashSet<>();
        for (UUID perfilId : perfisIds) {
            Role role = roleRepository.findById(perfilId)
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Perfil não encontrado nesta escola."));
            GuardaConcessao.exigirPapelConcedivel(role.getName());
            role.getPermissions().forEach(perm -> concedidas.add(perm.getName()));
            user.getRoles().add(role);
        }
        // Ninguem monta para outro (nem para si) um conjunto de perfis com
        // mais permissoes do que quem esta atribuindo tem.
        GuardaConcessao.exigirNaoAmpliar(concedidas);
    }

    private boolean ehOProprioUsuario(UUID id) {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal()
                instanceof br.com.alfaschool.backend.security.jwt.AuthenticatedUser principal)) {
            return false;
        }
        return id.equals(principal.userId());
    }

    private UserAccount carregar(UUID id) {
        UUID tenantId = requiredTenant();
        return userRepository.findByIdAndTenantId(id, tenantId)
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public List<String> userRoles(UUID userId) {
        UUID tenantId = requiredTenant();
        UserAccount user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return user.getRoles().stream().map(Role::getName).toList();
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }

    private UserResponse map(UserAccount user) {
        return UserResponse.from(user);
    }
}
