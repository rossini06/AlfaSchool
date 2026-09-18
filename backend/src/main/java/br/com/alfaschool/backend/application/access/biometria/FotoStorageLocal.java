package br.com.alfaschool.backend.application.access.biometria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Implementacao em disco local do {@link FotoStorage}.
 *
 * Serve o laboratorio e a escola de uma unidade so'. Com mais de uma
 * replica do backend isto precisa virar S3/MinIO — a interface ja esta
 * pronta para isso; o que nao pode e' o codigo de negocio saber a
 * diferenca.
 *
 * A chave e' "<tenant>/<uuid>.<ext>". Toda leitura resolve o caminho e
 * confere que ele continua DENTRO do diretorio base: uma chave com ".."
 * viraria leitura arbitraria de arquivo do servidor.
 */
@Component
public class FotoStorageLocal implements FotoStorage {

    private static final Logger log = LoggerFactory.getLogger(FotoStorageLocal.class);

    private final Path base;
    private final long tamanhoMaximo;

    public FotoStorageLocal(@Value("${app.access.foto-dir:/tmp/alfaschool-fotos}") String diretorio,
                            @Value("${app.access.foto-max-bytes:2097152}") long tamanhoMaximo) {
        this.base = Paths.get(diretorio).toAbsolutePath().normalize();
        this.tamanhoMaximo = tamanhoMaximo;
    }

    @Override
    public String salvar(UUID tenantId, byte[] conteudo) {
        ImagemValidador.Formato formato = ImagemValidador.validar(conteudo, tamanhoMaximo);
        String extensao = formato == ImagemValidador.Formato.PNG ? "png" : "jpg";
        String chave = tenantId + "/" + UUID.randomUUID() + "." + extensao;
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar a foto: " + e.getMessage(), e);
        }
        return chave;
    }

    @Override
    public byte[] ler(String chave) {
        Path origem = resolver(chave);
        try {
            return Files.readAllBytes(origem);
        } catch (IOException e) {
            throw new IllegalStateException("Foto não encontrada no armazenamento: " + chave, e);
        }
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException e) {
            // Falha ao apagar nao pode derrubar o fluxo de negocio, mas
            // precisa aparecer: foto orfa de crianca e' passivo de LGPD.
            log.warn("Não foi possível remover a foto {}: {}", chave, e.getMessage());
        }
    }

    @Override
    public boolean existe(String chave) {
        return Files.isRegularFile(resolver(chave));
    }

    private Path resolver(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("Chave de foto vazia.");
        }
        Path resolvido = base.resolve(chave).normalize();
        if (!resolvido.startsWith(base)) {
            throw new IllegalArgumentException("Chave de foto inválida.");
        }
        return resolvido;
    }

    public Path diretorioBase() {
        return base;
    }
}
