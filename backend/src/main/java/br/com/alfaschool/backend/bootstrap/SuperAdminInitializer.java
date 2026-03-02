package br.com.alfaschool.backend.bootstrap;

import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.config.AdminProperties;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@EnableConfigurationProperties(AdminProperties.class)
public class SuperAdminInitializer implements CommandLineRunner {

    private static final String MASTER_DOCUMENT = "MASTER";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public SuperAdminInitializer(TenantRepository tenantRepository,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 AdminProperties adminProperties) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(String... args) {
        Tenant masterTenant = tenantRepository.findByDocument(MASTER_DOCUMENT)
                .orElseGet(this::createMasterTenant);

        Role superAdminRole = roleRepository.findByTenantIdAndNameIgnoreCase(masterTenant.getTenantId(), "SUPER_ADMIN")
                .orElseGet(() -> createSuperAdminRole(masterTenant.getTenantId()));

        if (!StringUtils.hasText(adminProperties.password())) {
            return;
        }

        userRepository.findByTenantIdAndEmailIgnoreCase(masterTenant.getTenantId(), adminProperties.email())
                .ifPresentOrElse(
                        user -> {
                        },
                        () -> createSuperAdminUser(masterTenant.getTenantId(), superAdminRole)
                );
    }

    private Tenant createMasterTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("AlfaSchool Master");
        tenant.setDocument(MASTER_DOCUMENT);
        tenant.setActive(true);
        tenant.setTenantId(UUID.randomUUID());
        return tenantRepository.save(tenant);
    }

    private Role createSuperAdminRole(UUID tenantId) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("SUPER_ADMIN");
        role.setDescription("Perfil administrativo principal");
        return roleRepository.save(role);
    }

    private void createSuperAdminUser(UUID tenantId, Role superAdminRole) {
        UserAccount user = new UserAccount();
        user.setTenantId(tenantId);
        user.setName(adminProperties.name());
        user.setEmail(adminProperties.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(adminProperties.password()));
        user.setMustChangePassword(true);
        user.setActive(true);
        user.getRoles().add(superAdminRole);
        userRepository.save(user);
    }
}
