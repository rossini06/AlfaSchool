package br.com.alfaschool.backend.application.access.biometria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Assina o acesso a uma foto por tempo curto.
 *
 * <h2>Por que assinatura e nao JWT no cabecalho</h2>
 * A foto e' exibida por uma tag {@code <img>}, e {@code <img>} nao envia
 * cabecalho nenhum. Sobram duas saidas: cookie de sessao, que nao serve
 * porque a TV do painel nao faz login, ou URL assinada. Sem uma das duas,
 * ou a foto fica aberta a quem tiver o link — permanente, porque a chave
 * nao muda — ou simplesmente nao aparece.
 *
 * <h2>O que a assinatura cobre</h2>
 * A chave da foto E o instante de expiracao. Trocar a chave na URL
 * invalida a assinatura, entao uma URL emitida para o aluno A nao serve
 * para o aluno B. E a URL morre sozinha: link vazado por print, historico
 * de navegador ou log de proxy para de funcionar em minutos.
 *
 * Nao e' controle de acesso por si so'. Quem decide se a pessoa pode ver
 * aquela crianca e' o endpoint que EMITE a URL; isto aqui so' garante que
 * a permissao concedida nao vire permanente nem transferivel.
 */
@Service
public class FotoUrlAssinada {

    /** Curto de proposito: o painel rebusca o estado a cada reconexao. */
    private static final Duration VALIDADE = Duration.ofMinutes(10);

    private final byte[] chaveHmac;
    private final String caminhoBase;

    public FotoUrlAssinada(@Value("${app.secret-key:}") String secretKey,
                           @Value("${app.access.foto-url-base:/api/v1/access/fotos}") String caminhoBase) {
        if (secretKey == null || secretKey.length() < 32) {
            throw new IllegalStateException(
                    "app.secret-key ausente ou curta demais para assinar URL de foto.");
        }
        // Namespace proprio: a mesma passphrase cifra senha de equipamento;
        // derivar por contexto evita que os dois usos compartilhem chave.
        this.chaveHmac = sha256(("foto-url:" + secretKey).getBytes(StandardCharsets.UTF_8));
        this.caminhoBase = caminhoBase;
    }

    /** URL relativa, pronta para ir num DTO e virar src de uma img. */
    public String emitir(String chaveFoto) {
        if (chaveFoto == null || chaveFoto.isBlank()) {
            return null;
        }
        long exp = Instant.now().plus(VALIDADE).getEpochSecond();
        return caminhoBase + "/" + url(chaveFoto)
                + "?exp=" + exp
                + "&sig=" + assinar(chaveFoto, exp);
    }

    /**
     * @return true se a assinatura confere E o prazo nao passou.
     *         Falha fechada: qualquer problema devolve false.
     */
    public boolean valida(String chaveFoto, String exp, String assinatura) {
        if (chaveFoto == null || exp == null || assinatura == null) {
            return false;
        }
        long expiraEm;
        try {
            expiraEm = Long.parseLong(exp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiraEm) {
            return false;
        }
        // Comparacao em tempo constante: comparar com equals abriria margem
        // para descobrir a assinatura byte a byte pelo tempo de resposta.
        return MessageDigest.isEqual(
                assinar(chaveFoto, expiraEm).getBytes(StandardCharsets.UTF_8),
                assinatura.getBytes(StandardCharsets.UTF_8));
    }

    private String assinar(String chaveFoto, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveHmac, "HmacSHA256"));
            byte[] bruto = mac.doFinal((chaveFoto + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bruto);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar URL de foto", e);
        }
    }

    private static byte[] sha256(byte[] entrada) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(entrada);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de assinatura", e);
        }
    }

    private static String url(String valor) {
        return java.net.URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
