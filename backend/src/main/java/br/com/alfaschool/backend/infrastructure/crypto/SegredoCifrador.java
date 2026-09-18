package br.com.alfaschool.backend.infrastructure.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Cifra segredos operacionais em repouso: senha de equipamento Control iD,
 * credencial de provedor de WhatsApp, senha SMTP.
 *
 * Nos sistemas anteriores do grupo a senha do leitor ficava em texto plano
 * no banco e era devolvida em claro para o agente. Aqui o valor so' existe
 * decifrado no momento de falar com o equipamento.
 *
 * AES-256-GCM. Formato do blob: [IV 12 bytes][ciphertext+tag].
 * A chave vem de APP_SECRET_KEY e precisa de no minimo 32 caracteres.
 */
@Component
public class SegredoCifrador {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public SegredoCifrador(@Value("${app.secret-key:}") String secretKey) {
        if (secretKey == null || secretKey.length() < 32) {
            throw new IllegalStateException(
                    "app.secret-key (APP_SECRET_KEY) ausente ou com menos de 32 caracteres. "
                  + "Sem ela o sistema nao pode guardar senha de equipamento com seguranca.");
        }
        // SHA-256 normaliza qualquer passphrase para os 32 bytes exigidos pelo AES-256.
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de cifra", e);
        }
    }

    public byte[] cifrar(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] texto = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] saida = new byte[iv.length + texto.length];
            System.arraycopy(iv, 0, saida, 0, iv.length);
            System.arraycopy(texto, 0, saida, iv.length, texto.length);
            return saida;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar segredo", e);
        }
    }

    /**
     * Devolve null quando o blob nao abre — tipicamente chave trocada sem
     * reencriptar. Quem chama deve tratar como "sem senha", nunca como vazio.
     */
    public String decifrar(byte[] blob) {
        if (blob == null || blob.length <= IV_LEN) {
            return null;
        }
        try {
            byte[] iv = Arrays.copyOfRange(blob, 0, IV_LEN);
            byte[] texto = Arrays.copyOfRange(blob, IV_LEN, blob.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(texto), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
