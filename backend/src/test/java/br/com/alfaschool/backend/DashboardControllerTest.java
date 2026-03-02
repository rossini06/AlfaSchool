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
class DashboardControllerTest {

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
    private UserAccount userA;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        tenantRepository.deleteAll();

        tenantA = new Tenant();
        tenantA.setTenantId(UUID.randomUUID());
        tenantA.setName("Tenant Dashboard A");
        tenantA.setDocument("TD-A");
        tenantA.setActive(true);
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setTenantId(UUID.randomUUID());
        tenantB.setName("Tenant Dashboard B");
        tenantB.setDocument("TD-B");
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        Role roleUserA = new Role();
        roleUserA.setTenantId(tenantA.getTenantId());
        roleUserA.setName("USER");
        roleUserA.setDescription("Usuário base");
        roleUserA = roleRepository.save(roleUserA);

        Role roleUserB = new Role();
        roleUserB.setTenantId(tenantB.getTenantId());
        roleUserB.setName("USER");
        roleUserB.setDescription("Usuário base");
        roleUserB = roleRepository.save(roleUserB);

        userA = new UserAccount();
        userA.setTenantId(tenantA.getTenantId());
        userA.setName("User Dashboard A");
        userA.setEmail("dash@tenant-a.com");
        userA.setPassword(passwordEncoder.encode("123456"));
        userA.getRoles().add(roleUserA);
        userA = userRepository.save(userA);

        UserAccount userB = new UserAccount();
        userB.setTenantId(tenantB.getTenantId());
        userB.setName("User Dashboard B");
        userB.setEmail("dash@tenant-b.com");
        userB.setPassword(passwordEncoder.encode("123456"));
        userB.getRoles().add(roleUserB);
        userRepository.save(userB);
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarDashboardComRespostaPadrao() throws Exception {
        String token = loginAndGetToken(tenantA.getTenantId(), userA.getEmail(), "123456");

        String response = mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.get("status").asInt()).isEqualTo(200);
        assertThat(json.get("message").asText()).isEqualTo("Dashboard loaded successfully");
        assertThat(json.get("data").has("stats")).isTrue();
        assertThat(json.get("data").has("alerts")).isTrue();
        assertThat(json.get("data").has("systemHealth")).isTrue();
    }

    @Test
    void deveBloquearQuandoTenantHeaderNaoCombinaComToken() throws Exception {
        String token = loginAndGetToken(tenantA.getTenantId(), userA.getEmail(), "123456");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", tenantB.getTenantId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveIsolarDadosPorTenantNoDashboard() throws Exception {
        String token = loginAndGetToken(tenantA.getTenantId(), userA.getEmail(), "123456");

        String response = mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode stats = objectMapper.readTree(response).get("data").get("stats");
        long totalStaff = 0;
        for (JsonNode stat : stats) {
            if ("totalStaff".equals(stat.get("key").asText())) {
                totalStaff = stat.get("value").asLong();
            }
        }
        assertThat(totalStaff).isEqualTo(1L);
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
