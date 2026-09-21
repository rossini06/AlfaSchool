package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.domain.role.Permission;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.PermissionRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/** Regras da tela de perfis e permissoes. */
@Service
public class PerfilAdminService {

    private static final Logger log = LoggerFactory.getLogger(PerfilAdminService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UsuarioPermissaoExtraRepository extraRepository;
    private final br.com.alfaschool.backend.application.shared.AuditService auditService;

    public PerfilAdminService(RoleRepository roleRepository,
                              PermissionRepository permissionRepository,
                              UserRepository userRepository,
                              UsuarioPermissaoExtraRepository extraRepository,
                              br.com.alfaschool.backend.application.shared.AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.extraRepository = extraRepository;
        this.auditService = auditService;
    }

    private UUID tenant() {
        UUID t = TenantContext.getTenantId();
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return t;
    }

    /**
     * Catalogo agrupado por AREA (o prefixo do nome), que e' como a tela
     * mostra: uma secao por area, com as acoes dentro. Marcar permissao a
     * permissao numa lista de 46 seria ilegivel.
     */
    public Map<String, Object> catalogo() {
        Map<String, List<Map<String, String>>> porArea = new LinkedHashMap<>();
        for (Permissao p : Permissao.values()) {
            String area = area(p);
            porArea.computeIfAbsent(area, k -> new ArrayList<>())
                    .add(Map.of("chave", p.name(), "descricao", p.getDescricao()));
        }
        return Map.of("areas", porArea, "total", Permissao.values().length);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        UUID tenantId = tenant();
        return roleRepository.findAll().stream()
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> {
                    Set<String> chaves = new TreeSet<>();
                    r.getPermissions().forEach(p -> chaves.add(p.getName()));
                    PerfilEscolar padrao = PerfilEscolar.porNome(r.getName());
                    // Perfil de sistema ainda sem vinculo gravado exibe o
                    // conjunto do catalogo; senao a tela mostraria "0
                    // permissoes" para quem na pratica tem acesso.
                    if (chaves.isEmpty() && padrao != null) {
                        padrao.getPermissoes().forEach(p -> chaves.add(p.name()));
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("nome", r.getName());
                    m.put("rotulo", r.getRotulo() != null ? r.getRotulo() : r.getName());
                    m.put("descricao", r.getDescription());
                    m.put("sistema", r.isSistema());
                    m.put("permissoes", chaves);
                    return m;
                })
                .toList();
    }

    @Transactional
    public void atualizarPermissoes(UUID roleId, List<String> chaves) {
        UUID tenantId = tenant();
        Role role = roleRepository.findById(roleId)
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));

        if ("SUPER_ADMIN".equalsIgnoreCase(role.getName())) {
            // Editar o SUPER_ADMIN e' o caminho mais curto para trancar todo
            // mundo do lado de fora, inclusive quem esta editando.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O perfil Super Admin não pode ser alterado.");
        }

        Set<Permissao> validas = new LinkedHashSet<>();
        for (String chave : chaves) {
            try {
                validas.add(Permissao.valueOf(chave.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Permissão desconhecida: " + chave);
            }
        }

        // Quem edita a matriz nao pode dar a um perfil permissao que ele
        // proprio nao tem (senao concede a si por tabela e relogar).
        GuardaConcessao.exigirNaoAmpliar(validas.stream().map(Enum::name).toList());

        // A partir da primeira edicao pela tela, o que estiver gravado e' o
        // que vale — inclusive lista vazia. Sem isto, esvaziar um perfil de
        // sistema o fazia voltar ao padrao (o fallback do PermissaoService).
        role.setPersonalizado(true);
        role.getPermissions().clear();
        for (Permissao p : validas) {
            Permission entidade = permissionRepository
                    .findByTenantIdAndNameIgnoreCase(tenantId, p.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Catálogo de permissões incompleto nesta escola. Reinicie o sistema."));
            role.getPermissions().add(entidade);
        }
        roleRepository.save(role);
        auditService.registrarAcao("PERFIL_PERMISSOES_ALTERADAS", "ROLE", role.getId());
        log.info("Perfil {} do tenant {} agora tem {} permissoes", role.getName(), tenantId, validas.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> doUsuario(UUID userId) {
        UUID tenantId = tenant();
        UserAccount usuario = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Set<String> doPerfil = new TreeSet<>();
        for (Role papel : usuario.getRoles()) {
            papel.getPermissions().forEach(p -> doPerfil.add(p.getName()));
            PerfilEscolar padrao = PerfilEscolar.porNome(papel.getName());
            if (papel.getPermissions().isEmpty() && padrao != null) {
                padrao.getPermissoes().forEach(p -> doPerfil.add(p.name()));
            }
        }

        Set<String> extras = new TreeSet<>();
        extraRepository.findByTenantIdAndUserIdAndDeletedFalse(tenantId, userId)
                .forEach(e -> extras.add(e.getPermissao().name()));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("perfis", usuario.getRoles().stream().map(Role::getName).sorted().toList());
        // A tela marca estas como fixas e desabilitadas: desmarcar exigiria
        // trocar o perfil, e um checkbox que nao obedece e' pior que ausente.
        m.put("doPerfil", doPerfil);
        m.put("extras", extras);
        return m;
    }

    /**
     * Substitui as extras de uma pessoa.
     *
     * Descarta o que o perfil ja' concede: guardar extra redundante faria a
     * tela mentir sobre o que foi concedido individualmente, e a limpeza no
     * dia em que o perfil mudar viraria adivinhacao.
     */
    @Transactional
    public void substituirExtras(UUID userId, List<String> chaves, String motivo) {
        UUID tenantId = tenant();
        UserAccount usuario = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Set<String> doPerfil = (Set<String>) doUsuario(userId).get("doPerfil");
        UUID autor = br.com.alfaschool.backend.application.access.retirada.ContextoAcesso.userIdOuNulo();

        // Extra e' concessao direta: mesma regra — nao se concede o que nao
        // se tem. Fecha a via de dar PERFIS_GERIR/FINANCEIRO a si mesmo.
        GuardaConcessao.exigirNaoAmpliar(chaves.stream()
                .map(c -> c == null ? "" : c.toUpperCase()).toList());

        extraRepository.deleteByTenantIdAndUserId(tenantId, userId);

        for (String chave : chaves) {
            Permissao p;
            try {
                p = Permissao.valueOf(chave.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permissão desconhecida: " + chave);
            }
            if (doPerfil.contains(p.name())) {
                continue;
            }
            UsuarioPermissaoExtra extra = new UsuarioPermissaoExtra();
            extra.setTenantId(tenantId);
            extra.setUserId(userId);
            extra.setPermissao(p);
            extra.setMotivo(motivo);
            extra.setConcedidoPor(autor);
            extra.setConcedidoEm(Instant.now());
            extraRepository.save(extra);
        }
        auditService.registrarAcao("USUARIO_PERMISSOES_EXTRAS_ALTERADAS", "USER", usuario.getId());
        log.info("Permissoes extras do usuario {} atualizadas por {}", usuario.getId(), autor);
    }

    /** Prefixo do nome: ACESSO_PAINEL_VER -> ACESSO. */
    private String area(Permissao p) {
        String nome = p.name();
        int corte = nome.indexOf('_');
        return corte > 0 ? nome.substring(0, corte) : nome;
    }
}
