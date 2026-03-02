package br.com.alfaschool.backend.application.user;

import br.com.alfaschool.backend.application.user.dto.CreateUserRequest;
import br.com.alfaschool.backend.application.user.dto.UserResponse;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
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
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setLocked(false);
        user.setFailedAttempts(0);

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

        user.getRoles().add(role);
        return map(userRepository.save(user));
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
        return new UserResponse(
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }
}
