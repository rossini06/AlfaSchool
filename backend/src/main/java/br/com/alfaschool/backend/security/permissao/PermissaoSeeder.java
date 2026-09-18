package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.domain.role.Permission;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.infrastructure.persistence.repository.PermissionRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Garante que cada escola tenha o catalogo de permissoes e os perfis do
 * sistema.
 *
 * <h2>Por que no boot e nao numa migration</h2>
 * Permissao e perfil sao POR TENANT. Uma migration semeia o que existe no
 * dia em que roda; a escola cadastrada amanha nasceria sem perfil nenhum e
 * seus usuarios sem acesso a nada. Rodando no boot, toda escola — inclusive
 * a que acabou de ser criada — recebe o catalogo.
 *
 * <h2>O que ele NAO faz</h2>
 * Nao remove permissao que a escola tirou de um perfil, e nao apaga perfil
 * que ela criou. Semear nao pode desfazer decisao de quem usa o sistema:
 * so' acrescenta o que falta.
 */
@Component
@Order(20)
public class PermissaoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissaoSeeder.class);

    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissaoSeeder(TenantRepository tenantRepository,
                           PermissionRepository permissionRepository,
                           RoleRepository roleRepository) {
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (Boolean.TRUE.equals(tenant.getDeleted())) {
                continue;
            }
            try {
                semear(tenant.getTenantId());
            } catch (RuntimeException e) {
                // Uma escola com dado inconsistente nao pode impedir o boot
                // nem deixar as outras sem perfil.
                log.error("Falha ao semear permissoes do tenant {}: {}",
                        tenant.getTenantId(), e.toString());
            }
        }
    }

    /** Idempotente: pode rodar em todo boot. */
    @Transactional
    public void semear(UUID tenantId) {
        Map<Permissao, Permission> catalogo = new HashMap<>();
        int criadas = 0;

        for (Permissao p : Permissao.values()) {
            Permission entidade = permissionRepository
                    .findByTenantIdAndNameIgnoreCase(tenantId, p.name())
                    .orElse(null);
            if (entidade == null) {
                entidade = new Permission();
                entidade.setTenantId(tenantId);
                entidade.setName(p.name());
                entidade.setDescription(p.getDescricao());
                entidade = permissionRepository.save(entidade);
                criadas++;
            } else if (entidade.getDescription() == null
                    || !entidade.getDescription().equals(p.getDescricao())) {
                entidade.setDescription(p.getDescricao());
                entidade = permissionRepository.save(entidade);
            }
            catalogo.put(p, entidade);
        }

        int perfisCriados = 0;
        for (PerfilEscolar perfil : PerfilEscolar.values()) {
            Role role = roleRepository.findByTenantIdAndNameIgnoreCase(tenantId, perfil.name())
                    .orElse(null);
            boolean novo = role == null;
            if (novo) {
                role = new Role();
                role.setTenantId(tenantId);
                role.setName(perfil.name());
                perfisCriados++;
            }
            role.setRotulo(perfil.getRotulo());
            role.setDescription(perfil.getDescricao());
            role.setSistema(true);

            if (novo) {
                // So' um perfil NOVO recebe o conjunto padrao. Num perfil que
                // ja existe, a escola pode ter tirado permissao de proposito —
                // repor no boot desfaria a decisao dela em silencio.
                for (Permissao p : perfil.getPermissoes()) {
                    role.getPermissions().add(catalogo.get(p));
                }
            }
            roleRepository.save(role);
        }

        if (criadas > 0 || perfisCriados > 0) {
            log.info("Tenant {}: {} permissoes e {} perfis criados.", tenantId, criadas, perfisCriados);
        }
    }
}
