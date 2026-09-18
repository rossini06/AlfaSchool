package br.com.alfaschool.backend;

import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTenantIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

        @Autowired
        private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private UserAccount adminA;
    private UserAccount adminB;

    @BeforeEach
    void setUp() {
                auditLogRepository.deleteAll();
        userRepository.deleteAll();
                roleRepository.deleteAll();
        tenantRepository.deleteAll();

        tenantA = new Tenant();
        tenantA.setTenantId(UUID.randomUUID());
        tenantA.setName("Tenant A");
        tenantA.setDocument("A-123");
        tenantA.setActive(true);
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setTenantId(UUID.randomUUID());
        tenantB.setName("Tenant B");
        tenantB.setDocument("B-123");
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        Role roleA = new Role();
        roleA.setTenantId(tenantA.getTenantId());
        roleA.setName("ADMIN");
        roleA.setDescription("Administrador");
        roleA = roleRepository.save(roleA);

        Role roleB = new Role();
        roleB.setTenantId(tenantB.getTenantId());
        roleB.setName("ADMIN");
        roleB.setDescription("Administrador");
        roleB = roleRepository.save(roleB);

        // Os endpoints de usuario e de perfil exigem PERM_USUARIOS_* e
        // PERM_PERFIS_GERIR. "ADMIN" e' um papel legado que nao esta no
        // catalogo de perfis e por isso nao carrega permissao nenhuma: o
        // admin do tenant e' o DIRETOR, e e' esse perfil que da o acesso.
        Role diretorA = new Role();
        diretorA.setTenantId(tenantA.getTenantId());
        diretorA.setName("DIRETOR");
        diretorA.setDescription("Diretor");
        diretorA = roleRepository.save(diretorA);

        Role diretorB = new Role();
        diretorB.setTenantId(tenantB.getTenantId());
        diretorB.setName("DIRETOR");
        diretorB.setDescription("Diretor");
        diretorB = roleRepository.save(diretorB);

        adminA = new UserAccount();
        adminA.setTenantId(tenantA.getTenantId());
        adminA.setName("Admin A");
        adminA.setEmail("admin@tenant.com");
        adminA.setPassword(passwordEncoder.encode("123456"));
        adminA.getRoles().add(roleA);
        adminA.getRoles().add(diretorA);
        adminA = userRepository.save(adminA);

        adminB = new UserAccount();
        adminB.setTenantId(tenantB.getTenantId());
        adminB.setName("Admin B");
        adminB.setEmail("admin@tenant.com");
        adminB.setPassword(passwordEncoder.encode("123456"));
        adminB.getRoles().add(roleB);
        adminB.getRoles().add(diretorB);
        adminB = userRepository.save(adminB);
    }

    @Test
    void deveAutenticarComTenantCorreto() throws Exception {
        String payload = """
                {
                  "tenantId": "%s",
                  "email": "admin@tenant.com",
                  "password": "123456"
                }
                """.formatted(tenantA.getTenantId());

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        assertThat(data.get("accessToken").asText()).isNotBlank();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
    }

    @Test
    void deveAplicarIsolamentoDeTenant() throws Exception {
        String tokenA = loginAndGetToken(tenantA.getTenantId(), "admin@tenant.com", "123456");

        mockMvc.perform(get("/api/v1/users/%s/roles".formatted(adminB.getId()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveBloquearAposCincoTentativasInvalidas() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tenantId": "%s",
                                      "email": "admin@tenant.com",
                                      "password": "senha-errada"
                                    }
                                    """.formatted(tenantA.getTenantId())))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "%s",
                                  "email": "admin@tenant.com",
                                  "password": "123456"
                                }
                                """.formatted(tenantA.getTenantId())))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void deveCriarUsuario() throws Exception {
        String tokenA = loginAndGetToken(tenantA.getTenantId(), "admin@tenant.com", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Usuário",
                                  "email": "novo@tenant.com",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByTenantIdAndEmailIgnoreCase(tenantA.getTenantId(), "novo@tenant.com")).isTrue();
    }

    @Test
    void deveCriarERelacionarRole() throws Exception {
        String tokenA = loginAndGetToken(tenantA.getTenantId(), "admin@tenant.com", "123456");

        String roleResponse = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "COORDINATOR",
                                  "description": "Coordenação"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID roleId = UUID.fromString(objectMapper.readTree(roleResponse).get("data").get("id").asText());

        mockMvc.perform(post("/api/v1/users/%s/roles/%s".formatted(adminA.getId(), roleId))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        String rolesResponse = mockMvc.perform(get("/api/v1/users/%s/roles".formatted(adminA.getId()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode roles = objectMapper.readTree(rolesResponse).get("data");
        assertThat(roles.toString()).contains("COORDINATOR");
    }

                @Test
                void superAdminDeveLogarSemTenantId() throws Exception {
                                Role superAdminRole = new Role();
                                superAdminRole.setTenantId(tenantA.getTenantId());
                                superAdminRole.setName("SUPER_ADMIN");
                                superAdminRole.setDescription("Administrador Global");
                                superAdminRole = roleRepository.save(superAdminRole);

                                UserAccount superAdmin = new UserAccount();
                                superAdmin.setTenantId(tenantA.getTenantId());
                                superAdmin.setName("Super Admin");
                                superAdmin.setEmail("superadmin@alfaschool.com");
                                superAdmin.setPassword(passwordEncoder.encode("123456"));
                                superAdmin.getRoles().add(superAdminRole);
                                userRepository.save(superAdmin);

                                mockMvc.perform(post("/api/v1/auth/login")
                                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                                .content("""
                                                                                                                                {
                                                                                                                                        "email": "superadmin@alfaschool.com",
                                                                                                                                        "password": "123456"
                                                                                                                                }
                                                                                                                                """))
                                                                .andExpect(status().isOk());
                }

    private String loginAndGetToken(UUID tenantId, String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(tenantId, email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("data").get("accessToken").asText();
    }
}
