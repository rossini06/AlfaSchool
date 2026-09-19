package br.com.alfaschool.backend;

import br.com.alfaschool.backend.domain.professor.Professor;
import br.com.alfaschool.backend.domain.role.Permission;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.PermissionRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ProfessorRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.permissao.Permissao;
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

import java.util.List;
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
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ProfessorRepository professorRepository;

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
        permissionRepository.deleteAll();
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

        // O dashboard deixou de ser aberto a qualquer autenticado: cada
        // bloco depende de permissao. Um perfil sem permissao nenhuma
        // agora recebe 403 — e' o que o teste do responsavel verifica.
        Role roleUserA = perfilCompleto(tenantA.getTenantId());
        Role roleUserB = perfilCompleto(tenantB.getTenantId());

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

        professorRepository.save(professorDe(tenantA.getTenantId(), "Professora do Tenant A"));
        professorRepository.save(professorDe(tenantB.getTenantId(), "Professor do Tenant B"));
    }

    /** Perfil com as permissoes que o dashboard exige de quem opera a escola. */
    private Role perfilCompleto(UUID tenantId) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("USER");
        role.setDescription("Usuário base");
        for (Permissao p : List.of(Permissao.ESCOLA_VER, Permissao.ESCOLA_GERIR,
                Permissao.ALUNOS_VER, Permissao.MATRICULAS_VER, Permissao.FINANCEIRO_VER)) {
            Permission permissao = new Permission();
            permissao.setTenantId(tenantId);
            permissao.setName(p.name());
            permissao.setDescription(p.getDescricao());
            role.getPermissions().add(permissionRepository.save(permissao));
        }
        return roleRepository.save(role);
    }

    private Professor professorDe(UUID tenantId, String nome) {
        Professor professor = new Professor();
        professor.setTenantId(tenantId);
        professor.setNome(nome);
        professor.setStatus("ativo");
        return professor;
    }

    /**
     * O furo que este teste fecha: /dashboard respondia 200 para qualquer
     * autenticado, e um responsavel via a matricula NOMINAL dos filhos das
     * outras familias e a inadimplencia da escola.
     */
    @Test
    void responsavelSemPermissaoDeOperacaoNaoAcessaODashboard() throws Exception {
        Role perfilPortal = new Role();
        perfilPortal.setTenantId(tenantA.getTenantId());
        perfilPortal.setName("RESPONSAVEL");
        perfilPortal.setDescription("Portal da família");
        Permission portal = new Permission();
        portal.setTenantId(tenantA.getTenantId());
        portal.setName(Permissao.PORTAL_ACESSAR.name());
        portal.setDescription(Permissao.PORTAL_ACESSAR.getDescricao());
        perfilPortal.getPermissions().add(permissionRepository.save(portal));
        perfilPortal = roleRepository.save(perfilPortal);

        UserAccount responsavel = new UserAccount();
        responsavel.setTenantId(tenantA.getTenantId());
        responsavel.setName("Mãe de aluno");
        responsavel.setEmail("mae@tenant-a.com");
        responsavel.setPassword(passwordEncoder.encode("123456"));
        responsavel.getRoles().add(perfilPortal);
        userRepository.save(responsavel);

        String token = loginAndGetToken(tenantA.getTenantId(), "mae@tenant-a.com", "123456");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
        // Cada tenant tem exatamente um professor. Ver 2 significaria que o
        // dashboard esta somando dados de outra escola.
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
