package br.com.alfaschool.backend.application.tenant;

import br.com.alfaschool.backend.application.auth.AuthApplicationService;
import br.com.alfaschool.backend.application.auth.dto.LoginRequest;
import br.com.alfaschool.backend.application.auth.dto.SelecionarRedeRequest;
import br.com.alfaschool.backend.application.auth.dto.SelecionarRedeResponse;
import br.com.alfaschool.backend.security.jwt.JwtTokenProvider;
import br.com.alfaschool.backend.application.modulo.ModuloService;
import br.com.alfaschool.backend.application.tenant.dto.TenantCreateRequest;
import br.com.alfaschool.backend.application.tenant.dto.TenantResponse;
import br.com.alfaschool.backend.application.tenant.dto.TenantUpdateRequest;
import br.com.alfaschool.backend.domain.modulo.Modulo;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ModuloRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantModuloRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TenantServiceTest {

    @Autowired private TenantService tenantService;
    @Autowired private AuthApplicationService authService;
    @Autowired private ModuloService moduloService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ModuloRepository moduloRepository;
    @Autowired private TenantModuloRepository tenantModuloRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private AuditLogRepository auditLogRepository;

    /** A coluna document tem 40 caracteres; um UUID inteiro nao cabe. */
    private static String cnpjUnico() {
        return "CNPJ-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void catalogo() {
        tenantModuloRepository.deleteAll();
        moduloRepository.deleteAll();
        for (String codigo : List.of("ACCESS", "PEDAGOGICO", "PORTAL")) {
            Modulo m = new Modulo();
            m.setCodigo(codigo);
            m.setNome(codigo);
            moduloRepository.save(m);
        }
    }

    @Test
    void criarRedeDeixaElaProntaParaUso() {
        String cnpj = cnpjUnico();
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "Colégio Teste", cnpj, "Ana Diretora", "ANA@Teste.com", "senha-inicial-1",
                List.of("ACCESS", "PORTAL")));

        assertThat(rede.tenantId()).isNotNull();
        assertThat(rede.mestre()).isFalse();
        assertThat(rede.modulos()).containsExactly("ACCESS", "PORTAL");

        // Perfis padrao existem na hora, nao so' no proximo boot.
        assertThat(roleRepository.findByTenantIdAndNameIgnoreCase(rede.tenantId(), "DIRETOR")).isPresent();
        assertThat(roleRepository.findByTenantIdAndNameIgnoreCase(rede.tenantId(), "PORTARIA")).isPresent();

        // Administrador com o perfil de diretor e senha para trocar.
        UserAccount admin = userRepository.findByTenantIdAndEmailIgnoreCase(rede.tenantId(), "ana@teste.com").orElseThrow();
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches("senha-inicial-1", admin.getPassword())).isTrue();
        assertThat(admin.getRoles()).anyMatch(r -> r.getName().equals("DIRETOR"));

        assertThat(moduloService.codigosVigentes(rede.tenantId())).containsExactly("ACCESS", "PORTAL");
    }

    @Test
    void superadminNoTenantMestreVeTodasAsRedes() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "Vista", cnpjUnico(), "V", "v@v.com", "12345678", List.of("ACCESS")));
        Tenant mestre = tenantRepository.findByDocument("MASTER").orElseThrow();

        // E' assim que o superadmin chega: com o contexto no tenant mestre.
        TenantContext.setTenantId(mestre.getTenantId());
        try {
            assertThat(tenantService.list(PageRequest.of(0, 100)).getContent())
                    .extracting(TenantResponse::name)
                    .contains("AlfaSchool Master", "Vista");
            assertThat(tenantService.findById(rede.id()).modulos()).containsExactly("ACCESS");
            assertThat(tenantService.listarModulos(rede.id())).anyMatch(m -> m.codigo().equals("ACCESS") && m.contratado());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void cnpjRepetidoEhConflito() {
        String cnpj = cnpjUnico();
        tenantService.criar(new TenantCreateRequest("A", cnpj, "X", "x@a.com", "12345678", List.of()));

        assertThatThrownBy(() -> tenantService.criar(
                new TenantCreateRequest("B", cnpj, "Y", "y@b.com", "12345678", List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void definirModulosTrocaOConjuntoInteiro() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "C", cnpjUnico(), "X", "c@c.com", "12345678", List.of("ACCESS")));

        tenantService.definirModulos(rede.id(), List.of("PEDAGOGICO"));

        assertThat(moduloService.codigosVigentes(rede.tenantId())).containsExactly("PEDAGOGICO");
        assertThat(tenantService.listarModulos(rede.id()))
                .filteredOn(m -> m.codigo().equals("ACCESS"))
                .allMatch(m -> !m.contratado());
    }

    @Test
    void moduloDesconhecidoEhRejeitado() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "D", cnpjUnico(), "X", "d@d.com", "12345678", List.of()));

        assertThatThrownBy(() -> tenantService.definirModulos(rede.id(), List.of("INEXISTENTE")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void redeSuspensaNaoAceitaLogin() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "E", cnpjUnico(), "Eva", "eva@e.com", "senha-forte-1", List.of()));
        LoginRequest login = new LoginRequest(rede.tenantId(), "eva@e.com", "senha-forte-1");
        assertThat(authService.login(login, "127.0.0.1").accessToken()).isNotBlank();

        tenantService.changeStatus(rede.id(), "INATIVO");

        assertThatThrownBy(() -> authService.login(login, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("suspensa");

        tenantService.changeStatus(rede.id(), "ATIVO");
        assertThat(authService.login(login, "127.0.0.1").accessToken()).isNotBlank();
    }

    @Test
    void superadminEntraNaRedeEscolhidaEVoltaAoMestre() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "Escolhida", cnpjUnico(), "X", "x@escolhida.com", "12345678", List.of("ACCESS")));
        UserAccount superadmin = userRepository.findAllByEmailIgnoreCase("superadmin@alfaschool.com").get(0);

        SelecionarRedeResponse dentro = authService.selecionarRede(superadmin.getId(), new SelecionarRedeRequest(rede.tenantId()), "127.0.0.1");
        assertThat(dentro.tenantId()).isEqualTo(rede.tenantId());
        assertThat(dentro.mestre()).isFalse();
        assertThat(dentro.modulos()).containsExactly("ACCESS");
        assertThat(dentro.roles()).contains("SUPER_ADMIN");
        assertThat(dentro.permissoes()).contains("ALUNOS_VER", "ACESSO_PAINEL_VER");
        assertThat(jwtTokenProvider.parseToken(dentro.accessToken()).get("tenantId", String.class))
                .isEqualTo(rede.tenantId().toString());
        // A entrada fica na auditoria DA REDE. Com @Transactional(readOnly)
        // o INSERT era descartado em silencio e a tabela ficava vazia.
        assertThat(auditLogRepository.findByTenantIdAndAction(rede.tenantId(), "SAAS_ACESSO_REDE", PageRequest.of(0, 5)).getTotalElements())
                .isEqualTo(1);

        SelecionarRedeResponse volta = authService.selecionarRede(superadmin.getId(), new SelecionarRedeRequest(null), "127.0.0.1");
        assertThat(volta.mestre()).isTrue();
        assertThat(volta.tenantId()).isEqualTo(superadmin.getTenantId());
    }

    @Test
    void quemNaoEhSuperadminNaoTrocaDeRede() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "Fechada", cnpjUnico(), "Ana", "ana@fechada.com", "12345678", List.of()));
        UserAccount diretora = userRepository.findByTenantIdAndEmailIgnoreCase(rede.tenantId(), "ana@fechada.com").orElseThrow();

        assertThatThrownBy(() -> authService.selecionarRede(diretora.getId(), new SelecionarRedeRequest(rede.tenantId()), "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void redeSuspensaNaoRecebeOSuperadmin() {
        TenantResponse rede = tenantService.criar(new TenantCreateRequest(
                "Parada", cnpjUnico(), "X", "x@parada.com", "12345678", List.of()));
        tenantService.changeStatus(rede.id(), "INATIVO");
        UserAccount superadmin = userRepository.findAllByEmailIgnoreCase("superadmin@alfaschool.com").get(0);

        assertThatThrownBy(() -> authService.selecionarRede(superadmin.getId(), new SelecionarRedeRequest(rede.tenantId()), "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("suspensa");
    }

    @Test
    void tenantMestreNaoSeEdita() {
        Tenant mestre = tenantRepository.findByDocument("MASTER").orElseThrow();

        assertThatThrownBy(() -> tenantService.atualizar(mestre.getId(), new TenantUpdateRequest("Outro", "MASTER")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mestre");
        assertThatThrownBy(() -> tenantService.changeStatus(mestre.getId(), "INATIVO"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
