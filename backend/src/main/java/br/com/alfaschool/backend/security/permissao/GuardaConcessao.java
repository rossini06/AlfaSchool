package br.com.alfaschool.backend.security.permissao;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Impede escalada de privilegio: <b>ninguem concede o que nao tem</b>.
 *
 * <p>Confirmado na verificacao de seguranca: quem tinha "gerir usuarios"
 * atribuia a si mesmo o perfil de diretor (via {@code POST
 * /users/{id}/roles/{id}}) e ganhava permissoes que nao possuia; e no
 * tenant mestre atribuia a si o SUPER_ADMIN, virando administrador de todas
 * as escolas. A regra fecha os dois: o conjunto concedido — a um perfil, a
 * um usuario ou a si mesmo — precisa ser subconjunto do que QUEM CONCEDE ja
 * tem. O superadministrador legitimo (ROLE_SUPER_ADMIN no token) e' isento,
 * porque ele ja tem tudo.
 *
 * <p>As permissoes de quem chama saem das authorities do token (PERM_*),
 * que sao exatamente o que ele carrega naquele momento.
 */
public final class GuardaConcessao {

    private static final String PREFIXO_PERM = "PERM_";

    private GuardaConcessao() {
    }

    public static boolean chamadorEhSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
    }

    private static Set<String> permissoesDoChamador() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(PREFIXO_PERM))
                .map(a -> a.substring(PREFIXO_PERM.length()))
                .collect(Collectors.toSet());
    }

    /**
     * Recusa 403 se {@code permissoesAlvo} tiver alguma permissao que quem
     * chama nao possui. Nomes sem o prefixo {@code PERM_}.
     */
    public static void exigirNaoAmpliar(Collection<String> permissoesAlvo) {
        if (chamadorEhSuperAdmin()) {
            return;
        }
        Set<String> minhas = permissoesDoChamador();
        List<String> excedentes = permissoesAlvo.stream()
                .filter(p -> !minhas.contains(p))
                .distinct()
                .sorted()
                .toList();
        if (!excedentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não pode conceder permissões que você mesmo não tem: " + String.join(", ", excedentes)
                            + ". Peça a quem tiver essas permissões.");
        }
    }

    /**
     * SUPER_ADMIN nao se atribui pela API: nasce do bootstrap. Bloqueia para
     * todos, menos para outro superadministrador (que ja e' o topo).
     */
    public static void exigirPapelConcedivel(String nomeDoPapel) {
        if (NomesDePerfilReservados.ehSuperAdmin(nomeDoPapel) && !chamadorEhSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O perfil Super Admin não pode ser atribuído pela tela.");
        }
    }
}
