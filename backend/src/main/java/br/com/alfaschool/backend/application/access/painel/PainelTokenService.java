package br.com.alfaschool.backend.application.access.painel;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Token da TV.
 *
 * O token em claro existe em dois lugares e so' dois: na resposta da
 * criacao (uma unica vez) e na propria TV. No banco fica o SHA-256. Se a
 * base vazar, o que vaza nao serve para assistir ao fluxo de fotos de
 * crianca.
 *
 * SHA-256 sem salt e' adequado AQUI, e nao seria para senha: o token tem
 * 256 bits de entropia aleatoria, entao nao ha' dicionario nem rainbow
 * table que ajude. Salt/KDF resolvem o problema de segredo escolhido por
 * humano, que nao e' o caso.
 */
@Service
public class PainelTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES_DO_TOKEN = 32;
    private static final int TAMANHO_PREFIXO = 8;

    /** Token em claro + o que se guarda dele. */
    public record TokenGerado(String tokenEmClaro, String tokenHash, String tokenPrefixo) {
    }

    public TokenGerado gerar() {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefixo = token.substring(0, Math.min(TAMANHO_PREFIXO, token.length()));
        return new TokenGerado(token, hash(token), prefixo);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resumo = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(resumo.length * 2);
            for (byte b : resumo) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e' obrigatorio em qualquer JRE; se faltar, o ambiente
            // esta' quebrado e nao ha' como validar token nenhum.
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
        }
    }

    /**
     * Comparacao em tempo constante. Comparar com equals() vazaria, pelo
     * tempo de resposta, quantos caracteres iniciais o atacante acertou.
     */
    public boolean confere(String tokenEmClaro, String tokenHashGuardado) {
        if (tokenEmClaro == null || tokenHashGuardado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(tokenEmClaro).getBytes(StandardCharsets.UTF_8),
                tokenHashGuardado.getBytes(StandardCharsets.UTF_8));
    }
}
