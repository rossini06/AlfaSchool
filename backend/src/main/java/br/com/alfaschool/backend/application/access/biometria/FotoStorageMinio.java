package br.com.alfaschool.backend.application.access.biometria;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Implementacao do {@link FotoStorage} em object storage (MinIO/S3).
 *
 * Serve o perfil com mais de uma replica de backend, onde o disco local de
 * uma instancia nao e' visto pelas outras. So' entra em cena quando
 * {@code app.access.storage=minio}; com o padrao (local) nem e' instanciada
 * (ver {@link FotoStorageLocal}). O codigo de negocio continua falando com a
 * interface {@link FotoStorage} e nao sabe a diferenca.
 *
 * A chave e' PLANA ({@code <tenant>_<uuid>.<ext>}), pelo mesmo motivo do
 * storage local: a barra viraria %2F e o firewall rejeitaria a URL da foto.
 */
@Component
@ConditionalOnProperty(name = "app.access.storage", havingValue = "minio")
public class FotoStorageMinio implements FotoStorage {

    private static final Logger log = LoggerFactory.getLogger(FotoStorageMinio.class);

    private final MinioClient client;
    private final String bucket;
    private final long tamanhoMaximo;

    public FotoStorageMinio(@Value("${app.access.minio.endpoint:http://minio:9000}") String endpoint,
                            @Value("${app.access.minio.access-key:}") String accessKey,
                            @Value("${app.access.minio.secret-key:}") String secretKey,
                            @Value("${app.access.minio.bucket:alfaschool-fotos}") String bucket,
                            @Value("${app.access.foto-max-bytes:2097152}") long tamanhoMaximo) {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "app.access.minio.access-key/secret-key ausentes com app.access.storage=minio.");
        }
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        this.tamanhoMaximo = tamanhoMaximo;
    }

    /**
     * Cria o bucket no boot se ainda nao existe — o storage tem de estar pronto
     * antes do primeiro upload. Com retry porque o container do MinIO pode ainda
     * estar subindo quando o backend inicializa (depends_on e' service_started,
     * nao healthy — a imagem do MinIO nao traz ferramenta de healthcheck).
     */
    @PostConstruct
    void garantirBucket() {
        RuntimeException ultimoErro = null;
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            try {
                boolean existe = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!existe) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Bucket de fotos '{}' criado no MinIO.", bucket);
                }
                return;
            } catch (Exception e) {
                ultimoErro = new IllegalStateException(
                        "Não foi possível preparar o bucket de fotos no MinIO: " + e.getMessage(), e);
                log.warn("MinIO ainda indisponível (tentativa {}/10): {}", tentativa, e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw ultimoErro;
    }

    @Override
    public String salvar(UUID tenantId, byte[] conteudo) {
        ImagemValidador.Formato formato = ImagemValidador.validar(conteudo, tamanhoMaximo);
        String extensao = formato == ImagemValidador.Formato.PNG ? "png" : "jpg";
        String contentType = formato == ImagemValidador.Formato.PNG ? "image/png" : "image/jpeg";
        String chave = tenantId + "_" + UUID.randomUUID() + "." + extensao;
        try (InputStream in = new ByteArrayInputStream(conteudo)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .stream(in, conteudo.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gravar a foto no MinIO: " + e.getMessage(), e);
        }
        return chave;
    }

    @Override
    public byte[] ler(String chave) {
        exigirChave(chave);
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(chave).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Foto não encontrada no armazenamento: " + chave, e);
        }
    }

    @Override
    public void remover(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(chave).build());
        } catch (Exception e) {
            // Nao derruba o fluxo de negocio, mas precisa aparecer: foto orfa
            // de crianca e' passivo de LGPD.
            log.warn("Não foi possível remover a foto {} do MinIO: {}", chave, e.getMessage());
        }
    }

    @Override
    public boolean existe(String chave) {
        if (chave == null || chave.isBlank()) {
            return false;
        }
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(chave).build());
            return true;
        } catch (ErrorResponseException e) {
            return false; // objeto nao existe
        } catch (Exception e) {
            // Falha fechada: na duvida, trata como ausente (o controller devolve 404).
            log.warn("Falha ao verificar a foto {} no MinIO: {}", chave, e.getMessage());
            return false;
        }
    }

    private static void exigirChave(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("Chave de foto vazia.");
        }
    }
}
