package br.com.alfaschool.backend.security;

import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.security.permissao.NomesDePerfilReservados;
import br.com.alfaschool.backend.security.permissao.SuperAdminGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * A escalada que estes testes fecham:
 *
 * <pre>
 *   1. usuario com PERFIS_GERIR cria um perfil chamado "super_admin"
 *   2. se atribui a ele
 *   3. faz logout e login
 *   4. recebe ROLE_SUPER_ADMIN e passa a listar e suspender TODAS as escolas
 * </pre>
 *
 * Nenhum passo e' anormal do ponto de vista do sistema: alguem com
 * permissao para gerir perfis geriu um perfil.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuperAdminGuardTest {

    private static final UUID TENANT_MESTRE = UUID.fromString("00000000-0000-0000-0000-00000000aaaa");
    private static final UUID ESCOLA = UUID.fromString("00000000-0000-0000-0000-00000000bbbb");

    @Mock private TenantRepository tenantRepository;

    private SuperAdminGuard guard;

    @BeforeEach
    void preparar() {
        Tenant mestre = new Tenant();
        mestre.setTenantId(TENANT_MESTRE);
        mestre.setDocument("MASTER");
        when(tenantRepository.findByDocument(anyString())).thenReturn(Optional.of(mestre));
        guard = new SuperAdminGuard(tenantRepository);
    }

    private UserAccount usuario(UUID tenantId, String... perfis) {
        UserAccount u = new UserAccount();
        u.setTenantId(tenantId);
        for (String nome : perfis) {
            Role r = new Role();
            r.setTenantId(tenantId);
            r.setName(nome);
            u.getRoles().add(r);
        }
        return u;
    }

    @Test
    @DisplayName("SUPER_ADMIN no tenant mestre e' legitimo")
    void superAdminDoMestreVale() {
        assertTrue(guard.ehSuperAdmin(usuario(TENANT_MESTRE, "SUPER_ADMIN")));
    }

    @Test
    @DisplayName("Perfil SUPER_ADMIN criado dentro de uma escola nao concede nada")
    void superAdminForjadoNaEscolaNaoVale() {
        UserAccount forjado = usuario(ESCOLA, "COORDENACAO", "SUPER_ADMIN");

        assertFalse(guard.ehSuperAdmin(forjado));
        // E, principalmente, o nome nao chega ao token: se chegasse, o
        // hasRole('ROLE_SUPER_ADMIN') dos controllers de SaaS concederia
        // acesso sem consultar o banco.
        assertFalse(guard.papeisParaToken(forjado).contains("SUPER_ADMIN"));
        assertTrue(guard.papeisParaToken(forjado).contains("COORDENACAO"));
    }

    @Test
    @DisplayName("Variacoes de escrita do nome reservado tambem sao barradas")
    void variacoesDoNomeSaoBarradas() {
        for (String variante : new String[]{"super_admin", "Super Admin", "SUPER-ADMIN", "  super admin  "}) {
            assertTrue(NomesDePerfilReservados.reservado(variante),
                    "deveria ser reservado: " + variante);
            assertFalse(guard.ehSuperAdmin(usuario(ESCOLA, variante)),
                    "nao deveria conceder: " + variante);
        }
    }

    @Test
    @DisplayName("Nome de perfil do sistema tambem e' reservado, para nao duplicar o que o seeder mantem")
    void perfisDeSistemaSaoReservados() {
        assertTrue(NomesDePerfilReservados.reservado("COORDENACAO"));
        assertTrue(NomesDePerfilReservados.reservado("secretaria"));
        assertFalse(NomesDePerfilReservados.reservado("COORDENACAO PEDAGOGICA"));
    }

    @Test
    @DisplayName("Sem tenant mestre cadastrado, ninguem e' superadministrador")
    void semMestreNinguemEhSuperAdmin() {
        when(tenantRepository.findByDocument(anyString())).thenReturn(Optional.empty());
        SuperAdminGuard semMestre = new SuperAdminGuard(tenantRepository);

        assertFalse(semMestre.ehSuperAdmin(usuario(TENANT_MESTRE, "SUPER_ADMIN")));
    }
}
