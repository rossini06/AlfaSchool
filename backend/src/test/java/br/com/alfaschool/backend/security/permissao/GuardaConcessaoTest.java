package br.com.alfaschool.backend.security.permissao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Trava a escalada de privilegio confirmada na verificacao: ninguem concede
 * o que nao tem, e SUPER_ADMIN nao se atribui pela tela.
 */
class GuardaConcessaoTest {

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarCom(String... authorities) {
        var lista = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p", lista));
    }

    @Test
    void concederOQueSeTemPassa() {
        autenticarCom("PERM_USUARIOS_VER", "PERM_USUARIOS_GERIR");
        assertThatCode(() -> GuardaConcessao.exigirNaoAmpliar(List.of("USUARIOS_VER")))
                .doesNotThrowAnyException();
    }

    @Test
    void concederOQueNaoSeTemEhBloqueado() {
        autenticarCom("PERM_USUARIOS_GERIR");
        assertThatThrownBy(() -> GuardaConcessao.exigirNaoAmpliar(List.of("PERFIS_GERIR", "FINANCEIRO_GERIR")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PERFIS_GERIR");
    }

    @Test
    void superAdminNaoSeAtribuiPelaTela() {
        autenticarCom("PERM_USUARIOS_GERIR");
        assertThatThrownBy(() -> GuardaConcessao.exigirPapelConcedivel("SUPER_ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Super Admin");
    }

    @Test
    void superadminEhIsento() {
        autenticarCom("ROLE_SUPER_ADMIN");
        assertThat(GuardaConcessao.chamadorEhSuperAdmin()).isTrue();
        assertThatCode(() -> GuardaConcessao.exigirNaoAmpliar(List.of("PERFIS_GERIR", "FINANCEIRO_GERIR")))
                .doesNotThrowAnyException();
        assertThatCode(() -> GuardaConcessao.exigirPapelConcedivel("SUPER_ADMIN"))
                .doesNotThrowAnyException();
    }
}
