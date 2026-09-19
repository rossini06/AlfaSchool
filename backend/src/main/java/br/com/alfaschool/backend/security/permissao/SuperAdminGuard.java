package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Decide quem e' superadministrador de verdade.
 *
 * <h2>Por que o nome do perfil nao basta</h2>
 * O sistema reconhecia o superadministrador comparando o NOME do perfil com
 * "SUPER_ADMIN". Esse nome vai para o claim {@code roles} do token e vira a
 * authority {@code ROLE_SUPER_ADMIN}, que abre {@code /saas/**} e
 * {@code /tenants/**} — listar e suspender TODAS as escolas.
 *
 * Quem tivesse PERFIS_GERIR criava um perfil com esse nome no proprio
 * tenant, se atribuia a ele, saia e entrava: virava administrador do SaaS
 * inteiro. Bloquear a criacao do nome (ver {@link NomesDePerfilReservados})
 * fecha a porta conhecida; esta classe fecha a regra.
 *
 * <h2>A regra</h2>
 * SUPER_ADMIN so' vale no tenant mestre, que e' criado pelo bootstrap e nao
 * pertence a escola nenhuma. Uma linha com esse nome em qualquer outro
 * tenant nao concede nada — tenha ela vindo da tela, de uma importacao ou
 * de um INSERT direto no banco.
 */
@Component
public class SuperAdminGuard {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminGuard.class);

    /** Mesmo documento usado pelo SuperAdminInitializer para achar o mestre. */
    private static final String DOCUMENTO_DO_MESTRE = "MASTER";

    private final TenantRepository tenantRepository;

    /**
     * O tenant mestre e' criado uma vez no boot e nao muda de id. Consultar
     * o banco a cada login so' para descobrir isso nao paga.
     */
    private volatile UUID mestre;

    public SuperAdminGuard(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public boolean ehSuperAdmin(UserAccount usuario) {
        if (usuario == null || usuario.getTenantId() == null) {
            return false;
        }
        boolean temONome = usuario.getRoles().stream()
                .map(Role::getName)
                .anyMatch(NomesDePerfilReservados::ehSuperAdmin);
        if (!temONome) {
            return false;
        }
        UUID tenantMestre = tenantMestre();
        boolean legitimo = tenantMestre != null && tenantMestre.equals(usuario.getTenantId());
        if (!legitimo) {
            // Nao ha caminho normal que produza isto. Ou alguem escalou
            // privilegio, ou uma importacao trouxe um perfil que nao devia.
            log.warn("Perfil SUPER_ADMIN encontrado fora do tenant mestre e IGNORADO: usuario {} tenant {}",
                    usuario.getId(), usuario.getTenantId());
        }
        return legitimo;
    }

    /**
     * Nomes de perfil que podem virar authority {@code ROLE_*} no token.
     *
     * Um SUPER_ADMIN ilegitimo e' removido aqui, antes de o token ser
     * assinado — se entrasse no claim, o {@code hasRole('ROLE_SUPER_ADMIN')}
     * dos controllers de SaaS concederia acesso sem nem consultar o banco.
     */
    @Transactional(readOnly = true)
    public List<String> papeisParaToken(UserAccount usuario) {
        if (usuario == null) {
            return List.of();
        }
        boolean superAdmin = ehSuperAdmin(usuario);
        return usuario.getRoles().stream()
                .map(r -> r.getName().toUpperCase())
                .filter(nome -> superAdmin || !NomesDePerfilReservados.ehSuperAdmin(nome))
                .toList();
    }

    private UUID tenantMestre() {
        UUID cache = mestre;
        if (cache != null) {
            return cache;
        }
        UUID encontrado = tenantRepository.findByDocument(DOCUMENTO_DO_MESTRE)
                .map(Tenant::getTenantId)
                .orElse(null);
        mestre = encontrado;
        return encontrado;
    }
}
