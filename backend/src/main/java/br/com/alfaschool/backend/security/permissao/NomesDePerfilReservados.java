package br.com.alfaschool.backend.security.permissao;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nomes de perfil que a escola nao pode criar nem renomear.
 *
 * <h2>A escalada que isto fecha</h2>
 * O sistema reconhece o superadministrador pelo NOME do perfil. Como
 * qualquer pessoa com PERFIS_GERIR podia criar um perfil chamado
 * "super_admin" no proprio tenant e se atribuir a ele, bastava sair e
 * entrar de novo para receber todas as permissoes — e, por tabela,
 * {@code hasRole('ROLE_SUPER_ADMIN')}, que abre a administracao do SaaS:
 * listar e suspender TODAS as escolas do sistema.
 *
 * O caminho era autenticado, sem nada de anormal no log: do ponto de vista
 * do sistema, alguem com permissao para gerir perfis geriu um perfil.
 *
 * <h2>Por que tambem os perfis de sistema</h2>
 * COORDENADOR, SECRETARIA e os demais sao semeados por tenant e a escola
 * ajusta as permissoes deles. Deixar criar um segundo perfil com o mesmo
 * nome produz duas linhas que o seeder e as telas nao sabem distinguir.
 */
public final class NomesDePerfilReservados {

    /**
     * Tratado a parte: nao e' perfil de escola nenhuma. So' existe no tenant
     * mestre e e' criado pelo bootstrap.
     */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private static final Set<String> RESERVADOS =
            Stream.concat(Stream.of(SUPER_ADMIN),
                            Stream.of(PerfilEscolar.values()).map(Enum::name))
                    .map(n -> n.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());

    private NomesDePerfilReservados() {
    }

    public static boolean reservado(String nome) {
        return nome != null && RESERVADOS.contains(normalizar(nome));
    }

    public static boolean ehSuperAdmin(String nome) {
        return SUPER_ADMIN.equals(normalizar(nome));
    }

    /**
     * Espaco, hifen e underline sao equivalentes aqui. Sem isto,
     * "Super Admin" e "SUPER-ADMIN" passariam pela checagem e ainda assim
     * casariam com o {@code equalsIgnoreCase} de quem so' compara o nome.
     */
    private static String normalizar(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }
}
