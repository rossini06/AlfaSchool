package br.com.alfaschool.backend.application.access.controlid;

import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Token de webhook POR DISPOSITIVO.
 *
 * No AlfaGym existia UM segredo global: quem o obtivesse — um instalador,
 * um leitor comprometido, um log de proxy — podia postar evento em nome
 * de qualquer equipamento de qualquer escola. Aqui o segredo e' por
 * equipamento, guardado apenas como SHA-256, e a comparacao e' feita em
 * tempo constante.
 *
 * Por que SHA-256 e nao BCrypt: o webhook e' chamado a cada passagem na
 * catraca, varias vezes por segundo no horario de entrada. Um KDF lento
 * por requisicao viraria negacao de servico. O token e' aleatorio de 256
 * bits (nao e' senha humana), entao nao ha o que um ataque de dicionario
 * faca com o hash.
 */
@Service
public class WebhookTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DispositivoRepository dispositivos;

    public WebhookTokenService(DispositivoRepository dispositivos) {
        this.dispositivos = dispositivos;
    }

    /**
     * Gera (ou rotaciona) o token do dispositivo e devolve o valor em
     * CLARO. Este e' o unico momento em que ele existe legivel: o banco
     * so' guarda o hash, e nao ha endpoint de "consultar token".
     */
    @Transactional
    public String rotacionar(UUID tenantId, UUID dispositivoId) {
        Dispositivo d = dispositivos.findById(dispositivoId)
                .filter(x -> tenantId.equals(x.getTenantId()))
                .filter(x -> Boolean.FALSE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado."));

        byte[] bruto = new byte[32];
        RANDOM.nextBytes(bruto);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bruto);

        d.setWebhookTokenHash(hash(token));
        dispositivos.save(d);
        return token;
    }

    /** SHA-256 em hexadecimal minusculo, o formato gravado em webhook_token_hash. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    /**
     * Comparacao em TEMPO CONSTANTE.
     *
     * String.equals sai no primeiro byte diferente. Com um endpoint
     * publico que aceita milhares de tentativas, essa diferenca de tempo
     * permite descobrir o token byte a byte. MessageDigest.isEqual compara
     * o buffer inteiro sempre.
     *
     * Dispositivo sem token cadastrado responde false: fail-closed. Um
     * equipamento sem segredo nao pode ser "qualquer um passa".
     */
    public static boolean tokenConfere(String hashArmazenado, String tokenApresentado) {
        if (hashArmazenado == null || hashArmazenado.isBlank()
                || tokenApresentado == null || tokenApresentado.isBlank()) {
            return false;
        }
        byte[] esperado = hashArmazenado.toLowerCase(java.util.Locale.ROOT)
                .getBytes(StandardCharsets.UTF_8);
        byte[] recebido = hash(tokenApresentado).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, recebido);
    }
}
