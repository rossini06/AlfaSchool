package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.domain.role.Permission;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Permissoes EFETIVAS de um usuario.
 *
 * <p>Efetiva = uniao das permissoes de todos os perfis dele, mais as
 * extras concedidas individualmente. Um usuario pode ter mais de um perfil
 * — o professor que tambem coordena e' caso comum em escola pequena — e
 * nesse caso ele soma o que os dois dao.
 *
 * <p>SUPER_ADMIN e' o unico atalho: recebe tudo sem depender de vinculo,
 * porque e' o perfil que conserta o sistema quando o vinculo esta errado.
 */
@Service
public class PermissaoService {

    private static final Logger log = LoggerFactory.getLogger(PermissaoService.class);
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final UsuarioPermissaoExtraRepository extraRepository;

    public PermissaoService(UsuarioPermissaoExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    @Transactional(readOnly = true)
    public Set<Permissao> efetivasDe(UserAccount usuario) {
        if (usuario == null) {
            return Set.of();
        }

        boolean superAdmin = usuario.getRoles().stream()
                .anyMatch(r -> SUPER_ADMIN.equalsIgnoreCase(r.getName()));
        if (superAdmin) {
            return Set.of(Permissao.values());
        }

        Set<Permissao> efetivas = new LinkedHashSet<>();

        for (Role papel : usuario.getRoles()) {
            for (Permission p : papel.getPermissions()) {
                Permissao chave = converter(p.getName());
                if (chave != null) {
                    efetivas.add(chave);
                }
            }
            // Perfil de sistema sem vinculo gravado ainda assim vale pelo
            // catalogo: uma escola cujo seeder nao rodou nao pode ficar com
            // a coordenacao sem acesso a nada.
            PerfilEscolar perfil = PerfilEscolar.porNome(papel.getName());
            if (perfil != null && papel.getPermissions().isEmpty()) {
                efetivas.addAll(perfil.getPermissoes());
            }
        }

        extraRepository.findByTenantIdAndUserIdAndDeletedFalse(usuario.getTenantId(), usuario.getId())
                .forEach(extra -> efetivas.add(extra.getPermissao()));

        return efetivas;
    }

    /** Nomes das authorities ({@code PERM_*}) para o contexto de seguranca. */
    public List<String> authoritiesDe(UserAccount usuario) {
        return efetivasDe(usuario).stream().map(Permissao::authority).sorted().toList();
    }

    /** Nomes das permissoes, para ir no token e para o frontend. */
    public List<String> nomesDe(UserAccount usuario) {
        return efetivasDe(usuario).stream().map(Enum::name).sorted().toList();
    }

    private Permissao converter(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return Permissao.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Chave antiga no banco depois de um rename no enum. Ignorar e'
            // melhor do que derrubar o login de todo mundo.
            log.debug("Permissao desconhecida no banco, ignorada: {}", nome);
            return null;
        }
    }
}
