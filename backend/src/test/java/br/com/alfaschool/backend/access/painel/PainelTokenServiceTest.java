package br.com.alfaschool.backend.access.painel;

import br.com.alfaschool.backend.application.access.painel.PainelTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelTokenServiceTest {

    private final PainelTokenService service = new PainelTokenService();

    @Test
    @DisplayName("O token gerado nunca e igual ao que se guarda")
    void tokenEmClaroNaoEhOQueFicaGuardado() {
        PainelTokenService.TokenGerado gerado = service.gerar();

        assertNotEquals(gerado.tokenEmClaro(), gerado.tokenHash());
        assertEquals(64, gerado.tokenHash().length(), "SHA-256 em hexadecimal tem 64 caracteres");
        assertTrue(gerado.tokenEmClaro().startsWith(gerado.tokenPrefixo()));
        assertEquals(8, gerado.tokenPrefixo().length());
    }

    @Test
    @DisplayName("Dois tokens nunca colidem")
    void tokensSaoUnicos() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            assertTrue(vistos.add(service.gerar().tokenEmClaro()));
        }
    }

    @Test
    void confereTokenValido() {
        PainelTokenService.TokenGerado gerado = service.gerar();
        assertTrue(service.confere(gerado.tokenEmClaro(), gerado.tokenHash()));
    }

    @Test
    void recusaTokenInvalido() {
        PainelTokenService.TokenGerado gerado = service.gerar();

        assertFalse(service.confere("token-errado", gerado.tokenHash()));
        // Prefixo certo, resto errado: o caso que uma comparacao ingenua
        // vazaria pelo tempo de resposta.
        assertFalse(service.confere(gerado.tokenPrefixo() + "xxxxxxxxxxxx", gerado.tokenHash()));
        assertFalse(service.confere(null, gerado.tokenHash()));
        assertFalse(service.confere(gerado.tokenEmClaro(), null));
    }

    @Test
    void hashEhEstavel() {
        assertEquals(service.hash("abc"), service.hash("abc"));
        assertNotEquals(service.hash("abc"), service.hash("abd"));
    }
}
